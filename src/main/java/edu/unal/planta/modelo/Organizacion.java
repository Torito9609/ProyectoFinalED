package edu.unal.planta.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
     * <p>Los fallos que clasifique cada planta actualizan su ranking y el global. Si la partida ya
     * terminó, no hace nada.</p>
     */
    public void tick() {
        if (partidaTerminada()) {
            return;
        }
        for (Planta compania : companias) {
            compania.tick();
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
     * conserva.</p>
     */
    public void reiniciar() {
        for (Planta compania : companias) {
            compania.reiniciar();
        }
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
