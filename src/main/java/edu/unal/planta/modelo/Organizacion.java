package edu.unal.planta.modelo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * La organización: varias compañías, cada una con su propia {@link Planta}, y el ranking global
 * de fallos de todas ellas.
 *
 * <h2>Estructuras</h2>
 * <ul>
 *   <li><strong>Arreglo dinámico</strong> ({@code ArrayList<Planta>}): las compañías. El id de una
 *   compañía es su índice, así que buscarla por id cuesta O(1). El tick las recorre todas en
 *   orden, O(número de compañías). Las compañías no se eliminan, así que los ids no se corren.</li>
 *   <li><strong>Árbol rojo-negro</strong> ({@link RankingFallos}): el ranking global, con las
 *   máquinas de todas las compañías. Cada compañía lo mantiene al día con las suyas.</li>
 *   <li><strong>Conjunto</strong> ({@code TreeSet} declarado como {@code Set}): los nombres de las
 *   compañías, para que no se repitan. Responde "¿ya existe?" en O(log c).</li>
 *   <li><strong>Montículo binario</strong> ({@code PriorityQueue}): las solicitudes de overhaul
 *   aprobadas, de todas las compañías. La cima es la máquina en peor estado. Insertar y extraer
 *   cuestan O(log s).</li>
 *   <li><strong>Cola FIFO</strong> ({@code ArrayDeque}): los técnicos del fabricante libres,
 *   compartidos por todas las compañías.</li>
 * </ul>
 *
 * <p>Todas las plantas tienen siempre el mismo tick que la organización: las iniciales arrancan
 * juntas en 0, y una compañía creada después arranca en el tick actual.</p>
 *
 * <p>La jerarquía organización → compañías → máquinas tiene profundidad fija, por eso basta una
 * lista y no hace falta un árbol n-ario (ver la "Opción B" del documento de diseño).</p>
 *
 * <h2>Aleatoriedad reproducible</h2>
 * <p>La organización recibe una <strong>semilla maestra</strong>, y la semilla de cada compañía
 * se deriva de ella y de su id con {@link #semillaCompania(long, int)}. La misma semilla maestra
 * produce la misma simulación en todas las compañías.</p>
 *
 * <p>Como {@link Planta}, no es segura para hilos: el servicio serializa el acceso.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Organizacion {

    /**
     * Separación entre las semillas de compañías vecinas: la compañía {@code i} usa
     * {@code maestra × 1000 + i}.
     */
    private static final long FACTOR_SEMILLA = 1000L;

    private final long semillaMaestra;

    /** Arreglo dinámico: las compañías; el id de cada una es su índice. */
    private final List<Planta> companias;

    /** Árbol rojo-negro: las máquinas de todas las compañías, por puntaje de fallos. */
    private final RankingFallos rankingGlobal;

    /**
     * Conjunto: nombres de las compañías, normalizados (sin espacios a los lados y en minúsculas),
     * para que "planta norte" y "Planta Norte" cuenten como el mismo.
     */
    private final Set<String> nombres;

    private int tick;

    /** Modo de todas las compañías; una compañía creada después lo hereda. */
    private ModoSimulacion modo;

    /**
     * Montículo binario: solicitudes de overhaul aprobadas. Mayor puntaje congelado primero; en un
     * empate, la que llegó antes ({@link SolicitudOverhaul#COMPARADOR}).
     */
    private final PriorityQueue<SolicitudOverhaul> solicitudes;

    /** Todos los técnicos del fabricante, libres u ocupados, para mostrarlos. */
    private final List<TecnicoExterno> tecnicosExternos;

    /** Cola FIFO: técnicos del fabricante libres. */
    private final Queue<TecnicoExterno> externosLibres;

    /** Número de llegada de la próxima solicitud: desempata el montículo. */
    private long siguienteSecuencia;

    /**
     * Crea la organización con las compañías de la configuración, listas para el primer tick.
     *
     * <p>Complejidad: O(N log N), con N el total de máquinas, por las inserciones en el ranking
     * global.</p>
     *
     * @param semillaMaestra semilla de la que se derivan las de todas las compañías
     */
    public Organizacion(long semillaMaestra) {
        this.semillaMaestra = semillaMaestra;
        this.rankingGlobal = new RankingFallos(List.of(), null);
        this.companias = new ArrayList<>();
        this.nombres = new TreeSet<>();
        this.tick = 0;
        this.modo = ModoSimulacion.AUTOMATICO;
        this.solicitudes = new PriorityQueue<>(SolicitudOverhaul.COMPARADOR);
        this.tecnicosExternos = ConfiguracionPlanta.crearTecnicosExternos();
        this.externosLibres = new ArrayDeque<>(tecnicosExternos);
        this.siguienteSecuencia = 1;
        for (String nombre : ConfiguracionPlanta.NOMBRES_COMPANIAS) {
            crearCompania(nombre);
        }
    }

    /**
     * Indica si ya existe una compañía con ese nombre, sin distinguir mayúsculas.
     *
     * <p>Complejidad: O(log c), con c el número de compañías: una búsqueda en el conjunto.</p>
     *
     * @param nombre nombre a buscar
     * @return {@code true} si ya hay una compañía con ese nombre
     * @throws NullPointerException si el nombre es nulo
     */
    public boolean existeCompania(String nombre) {
        return nombres.contains(claveNombre(nombre));
    }

    /**
     * Crea una compañía nueva y la agrega al final de la lista.
     *
     * <p>Su id es el número de compañías que ya había, y su semilla se deriva de la maestra y de ese
     * id. Arranca con las máquinas y los técnicos de la configuración, conectada al ranking global,
     * en el tick actual de la organización y en su modo actual. No se puede eliminar.</p>
     *
     * <p>Complejidad: O(log c) por el conjunto de nombres, más crear la planta, que inserta sus
     * máquinas en el ranking global: O(m log N), con m sus máquinas y N el total.</p>
     *
     * @param nombre nombre visible; se guarda sin espacios a los lados
     * @return la compañía creada
     * @throws IllegalArgumentException si el nombre está vacío o ya existe
     * @throws NullPointerException     si el nombre es nulo
     */
    public Planta crearCompania(String nombre) {
        String clave = claveNombre(nombre);
        if (clave.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la compañía no puede estar vacío");
        }
        if (nombres.contains(clave)) {
            throw new IllegalArgumentException("Ya existe una compañía llamada " + nombre.strip());
        }
        int id = companias.size();
        Planta compania = new Planta(id, nombre.strip(), semillaCompania(semillaMaestra, id),
                rankingGlobal, tick);
        compania.cambiarModo(modo);
        companias.add(compania);
        nombres.add(clave);
        return compania;
    }

    /** Clave con que se compara un nombre: sin espacios a los lados y en minúsculas. */
    private static String claveNombre(String nombre) {
        return Objects.requireNonNull(nombre, "El nombre no puede ser nulo").strip().toLowerCase();
    }

    /**
     * Deriva la semilla de una compañía a partir de la maestra y del id de la compañía.
     *
     * <p>La regla es {@code maestra × 1000 + id}: con la maestra 7, las compañías usan 7000, 7001,
     * 7002, etc. Es determinista y solo depende del id, así que una compañía creada después tendrá
     * siempre la misma semilla.</p>
     *
     * @param semillaMaestra semilla de la organización
     * @param idCompania     id de la compañía
     * @return semilla de esa compañía
     */
    public static long semillaCompania(long semillaMaestra, int idCompania) {
        return semillaMaestra * FACTOR_SEMILLA + idCompania;
    }

    /**
     * Avanza un tick en toda la organización: ejecuta {@link Planta#tick()} en cada compañía, en el
     * orden de sus ids, e incrementa el tick de la organización.
     *
     * <p>Los fallos que clasifique cada planta actualizan su ranking y el global. Después avanzan
     * los overhauls en curso y, en modo automático, se asignan los siguientes con la regla de la
     * cima. Si la partida ya terminó, no hace nada.</p>
     */
    public void tick() {
        if (partidaTerminada()) {
            return;
        }
        for (Planta compania : companias) {
            compania.tick();
        }
        avanzarOverhauls();
        if (modo == ModoSimulacion.AUTOMATICO) {
            while (asignarUnOverhaul()) {
                // Cada vuelta despacha un camión; termina cuando falta técnico, solicitud o la
                // máquina de la cima no está libre.
            }
        }
        tick++;
    }

    /**
     * Cambia el modo de todas las compañías, con la simulación en marcha o en pausa.
     *
     * <p>Complejidad: O(c), con c el número de compañías.</p>
     *
     * @param nuevo modo nuevo
     * @throws NullPointerException si el modo es nulo
     */
    public void cambiarModo(ModoSimulacion nuevo) {
        modo = Objects.requireNonNull(nuevo, "El modo no puede ser nulo");
        for (Planta compania : companias) {
            compania.cambiarModo(nuevo);
        }
    }

    /**
     * Devuelve el modo de la organización.
     *
     * @return el modo de todas las compañías
     */
    public ModoSimulacion modo() {
        return modo;
    }

    /**
     * Devuelve todas las compañías a su estado inicial, con las mismas semillas.
     *
     * <p>Cada planta saca sus máquinas del ranking global antes de reiniciarlas y las vuelve a
     * meter después, así que el ranking global queda consistente sin rearmarlo. El modo se
     * conserva. El montículo de overhauls se vacía y los técnicos del fabricante quedan libres.</p>
     */
    public void reiniciar() {
        for (Planta compania : companias) {
            compania.reiniciar();
        }
        solicitudes.clear();
        externosLibres.clear();
        for (TecnicoExterno externo : tecnicosExternos) {
            externo.reiniciar();
            externosLibres.add(externo);
        }
        siguienteSecuencia = 1;
        tick = 0;
    }

    /**
     * Devuelve las compañías, en el orden de sus ids.
     *
     * <p>Complejidad: O(1): es una vista no modificable, no una copia.</p>
     *
     * @return lista no modificable de compañías
     */
    public List<Planta> companias() {
        return Collections.unmodifiableList(companias);
    }

    /**
     * Devuelve una compañía por su id.
     *
     * <p>Complejidad: O(1): el id es el índice en el arreglo.</p>
     *
     * @param id id de la compañía
     * @return la compañía con ese id
     * @throws IndexOutOfBoundsException si no existe una compañía con ese id
     */
    public Planta compania(int id) {
        return companias.get(id);
    }

    /**
     * Devuelve las máquinas de todas las compañías, de la que tiene más fallos a la que tiene menos.
     *
     * <p>Complejidad: O(N), con N el total de máquinas: un recorrido en orden del árbol.</p>
     *
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> rankingGlobal() {
        return rankingGlobal.enOrden();
    }

    /**
     * Devuelve las máquinas de todas las compañías con puntaje mayor o igual que el umbral, en
     * orden de ranking. Con el umbral de overhaul son las candidatas a overhaul.
     *
     * <p>Complejidad: O(log N + k), con k las máquinas devueltas: una consulta por rango en el
     * árbol, que se detiene en la primera máquina por debajo del umbral.</p>
     *
     * @param umbral puntaje mínimo
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> rankingGlobalConPuntajeMinimo(int umbral) {
        return rankingGlobal.conPuntajeMinimo(umbral);
    }

    // ---------------------------------------------------------------------------------
    // Overhauls
    // ---------------------------------------------------------------------------------

    /**
     * Aprueba un overhaul: la solicitud entra al montículo con el puntaje actual de la máquina,
     * congelado como clave.
     *
     * <p>Solo se aprueba si la máquina tiene recomendación de overhaul o de reemplazo, es decir si
     * su puntaje alcanzó el umbral: la recomendación filtra, y el jugador decide cuándo aprobar y si
     * paga. La máquina sigue operando mientras espera. El modo automático nunca aprueba.</p>
     *
     * <p>Complejidad: O(log s), con s las solicitudes en el montículo. La solicitud repetida se
     * detecta en O(1) con la marca de la máquina, sin recorrer el montículo.</p>
     *
     * @param idCompania    id de la compañía
     * @param indiceMaquina índice de la máquina en su inventario
     * @return la solicitud creada
     * @throws IndexOutOfBoundsException si la compañía o la máquina no existen
     * @throws IllegalStateException     si la compañía está en quiebra, si la máquina ya tiene una
     *                                   solicitud pendiente o si no tiene recomendación de overhaul
     */
    public SolicitudOverhaul aprobarOverhaul(int idCompania, int indiceMaquina) {
        Planta compania = compania(idCompania);
        Maquina maquina = compania.maquina(indiceMaquina);
        if (compania.enQuiebra()) {
            throw new IllegalStateException(compania.nombre() + " está en quiebra: no acepta acciones");
        }
        if (maquina.overhaulPendiente()) {
            throw new IllegalStateException(maquina.etiqueta() + " ya tiene un overhaul pendiente");
        }
        if (maquina.recomendacion() == Recomendacion.NINGUNA) {
            throw new IllegalStateException(maquina.etiqueta() + " no tiene recomendación de overhaul: "
                    + "puntaje " + maquina.puntajeFallos() + " de " + ConfiguracionPlanta.UMBRAL_OVERHAUL);
        }
        SolicitudOverhaul solicitud = new SolicitudOverhaul(idCompania, indiceMaquina,
                maquina.etiqueta(), maquina.puntajeFallos(), siguienteSecuencia++, tick);
        solicitudes.add(solicitud);
        maquina.marcarOverhaulPendiente(true);
        compania.anotar("Overhaul aprobado para " + maquina.etiqueta()
                + " (puntaje " + solicitud.puntajeCongelado() + ")");
        return solicitud;
    }

    /**
     * Botón del modo manual: despacha el camión del fabricante para la solicitud de la cima.
     *
     * <p>Ejecuta una vuelta de la misma operación que el tick automático. El jugador no elige la
     * solicitud: el montículo entrega su cima. Si no se puede, la razón queda en la bitácora de la
     * compañía de la cima.</p>
     *
     * <p>Complejidad: O(log s).</p>
     *
     * @return {@code true} si se despachó un overhaul
     * @throws IllegalStateException si la simulación está en modo automático
     */
    public boolean asignarSiguienteOverhaul() {
        if (modo != ModoSimulacion.MANUAL) {
            throw new IllegalStateException("La acción solo está disponible en modo manual");
        }
        boolean asignado = asignarUnOverhaul();
        if (!asignado && !solicitudes.isEmpty()) {
            SolicitudOverhaul cima = solicitudes.peek();
            Planta compania = companias.get(cima.idCompania());
            compania.anotar(externosLibres.isEmpty()
                    ? "El camión espera: no hay técnicos del fabricante libres"
                    : "El camión espera: " + cima.etiqueta() + " no está libre");
        }
        return asignado;
    }

    /**
     * Paso 2 del tick de la organización: a cada técnico del fabricante ocupado se le resta un
     * tick, y se cierran los overhauls que terminan. El técnico vuelve al final de su cola.
     */
    private void avanzarOverhauls() {
        for (TecnicoExterno externo : tecnicosExternos) {
            if (externo.estaLibre()) {
                continue;
            }
            externo.avanzar();
            if (!externo.trabajoTerminado()) {
                continue;
            }
            SolicitudOverhaul terminada = externo.liberar();
            companias.get(terminada.idCompania()).terminarOverhaul(terminada.indiceMaquina());
            externosLibres.add(externo);
        }
    }

    /**
     * Despacha un overhaul con la <strong>regla de la cima</strong>: se mira la cima del montículo
     * y, si su máquina está libre, se extrae, se desencola un técnico del fabricante y la máquina
     * entra en overhaul. Si la máquina de la cima no está libre, se espera: no se salta a la
     * segunda solicitud, porque eso sería sacar del montículo un elemento que no es la cima.
     *
     * <p>Descarte perezoso: una solicitud se extrae y se descarta cuando llega a la cima si su
     * compañía está en quiebra (para que no bloquee para siempre los overhauls de las demás) o si su
     * máquina fue reemplazada. La máquina nueva ocupa el mismo índice pero no tiene la marca de
     * overhaul pendiente, y así se reconoce la solicitud vieja sin buscarla en el montículo.</p>
     *
     * <p>Complejidad: O(log s) por la extracción.</p>
     *
     * @return {@code true} si se despachó un overhaul
     */
    private boolean asignarUnOverhaul() {
        while (!solicitudes.isEmpty()) {
            SolicitudOverhaul cima = solicitudes.peek();
            Planta compania = companias.get(cima.idCompania());
            Maquina maquina = compania.maquina(cima.indiceMaquina());
            if (compania.enQuiebra()) {
                maquina.marcarOverhaulPendiente(false);
                compania.anotar("Overhaul de " + cima.etiqueta() + " descartado: la compañía está en quiebra");
            } else if (!maquina.overhaulPendiente()) {
                compania.anotar("Overhaul de " + cima.etiqueta() + " descartado: la máquina fue reemplazada");
            } else {
                break;
            }
            solicitudes.poll();
        }
        if (externosLibres.isEmpty() || solicitudes.isEmpty()) {
            return false;
        }
        SolicitudOverhaul cima = solicitudes.peek();
        Planta compania = companias.get(cima.idCompania());
        Maquina maquina = compania.maquina(cima.indiceMaquina());
        if (!maquina.estaLibre()) {
            return false;
        }
        solicitudes.poll();
        TecnicoExterno externo = externosLibres.poll();
        externo.asignar(cima);
        maquina.marcarOverhaulPendiente(false);
        compania.iniciarOverhaul(cima.indiceMaquina(), externo.nombre());
        return true;
    }

    /**
     * Devuelve la cima del montículo: la solicitud que saldrá primero.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return la siguiente solicitud, o vacío si no hay ninguna
     */
    public Optional<SolicitudOverhaul> siguienteOverhaul() {
        return Optional.ofNullable(solicitudes.peek());
    }

    /**
     * Devuelve las solicitudes en el orden en que saldrán del montículo.
     *
     * <p>{@code PriorityQueue} no expone su arreglo interno (el orden de {@code toArray()} no está
     * garantizado), así que la vista no dibuja el árbol: muestra la cima y este orden de salida. Se
     * calcula extrayendo de una <strong>copia</strong>, sin tocar el montículo.</p>
     *
     * <p>Complejidad: O(s log s), con s las solicitudes; s es pequeño.</p>
     *
     * @return lista no modificable, de la primera a la última en salir
     */
    public List<SolicitudOverhaul> solicitudesEnOrdenDeSalida() {
        PriorityQueue<SolicitudOverhaul> copia = new PriorityQueue<>(solicitudes);
        List<SolicitudOverhaul> enOrden = new ArrayList<>(copia.size());
        while (!copia.isEmpty()) {
            enOrden.add(copia.poll());
        }
        return Collections.unmodifiableList(enOrden);
    }

    /**
     * Devuelve todos los técnicos del fabricante, libres u ocupados.
     *
     * @return lista no modificable
     */
    public List<TecnicoExterno> tecnicosExternos() {
        return Collections.unmodifiableList(tecnicosExternos);
    }

    /**
     * Devuelve los técnicos del fabricante libres, del primero al último en recibir un overhaul.
     *
     * @return copia inmutable de la cola
     */
    public List<TecnicoExterno> tecnicosExternosLibres() {
        return List.copyOf(externosLibres);
    }

    /**
     * Indica si la partida terminó: el tick llegó a {@link ConfiguracionPlanta#DURACION_PARTIDA}.
     * Después de eso {@link #tick()} no hace nada, hasta reiniciar.
     *
     * @return {@code true} si la partida terminó
     */
    public boolean partidaTerminada() {
        return tick >= ConfiguracionPlanta.DURACION_PARTIDA;
    }

    /**
     * Devuelve el tick actual de la organización.
     *
     * @return número de ticks ejecutados desde el inicio o el último reinicio
     */
    public int tickActual() {
        return tick;
    }

    /**
     * Devuelve la semilla maestra.
     *
     * @return semilla de la que se derivan las de las compañías
     */
    public long semillaMaestra() {
        return semillaMaestra;
    }
}
