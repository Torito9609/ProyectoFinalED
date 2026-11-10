package edu.unal.planta.modelo;

import edu.unal.planta.modelo.comandos.ComandoOperador;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fachada del modelo: contiene las estructuras de datos de una planta y ejecuta la simulación.
 *
 * <h2>Las estructuras</h2>
 * <table border="1">
 *   <caption>Estructuras que viven en esta clase</caption>
 *   <tr><th>Campo</th><th>Estructura</th><th>Para qué</th></tr>
 *   <tr><td>{@code inventario}</td><td>{@code ArrayList}</td><td>Las máquinas de la planta</td></tr>
 *   <tr><td>{@code eventos}</td><td>{@code ArrayDeque} como {@code Queue}</td>
 *       <td>Eventos por clasificar, en orden de llegada</td></tr>
 *   <tr><td>{@code tecnicosLibres}</td><td>{@code EnumMap} de dos {@code ArrayDeque} como
 *       {@code Queue}</td>
 *       <td>Técnicos disponibles, una cola FIFO por especialidad (Entrega 2)</td></tr>
 *   <tr><td>{@code ordenes}</td><td>{@link ColaPrioridadOrdenes}</td>
 *       <td>Órdenes de trabajo P1 a P4</td></tr>
 *   <tr><td>{@code pilaDeshacer}</td><td>{@code ArrayDeque} como pila</td>
 *       <td>Acciones del operador que se pueden revertir</td></tr>
 *   <tr><td>{@code ranking}</td><td>{@link RankingFallos} ({@code TreeSet}, árbol rojo-negro)</td>
 *       <td>Máquinas ordenadas por puntaje de fallos (Entrega 2)</td></tr>
 *   <tr><td>{@code etiquetas}</td><td>{@code TreeSet} declarado como {@code Set} (conjunto)</td>
 *       <td>Etiquetas de las máquinas, sin repetidos (Entrega 2)</td></tr>
 * </table>
 *
 * <p>La cola circular no aparece aquí porque cada {@link Maquina} es dueña de la suya, con sus
 * propias lecturas. La bitácora usa también un {@code ArrayDeque}, pero es un registro auxiliar y
 * no una de las cinco estructuras del enunciado: se eligió deque porque al llenarse hay que
 * descartar las líneas más viejas por el frente, en O(1).</p>
 *
 * <h2>Reglas de arquitectura</h2>
 * <p>Esta clase no importa nada de la vista ni del framework. Cuando el estado cambia, avisa a sus
 * {@link ObservadorPlanta} y no sabe quién escucha. Todo lo que entrega a la vista es de solo
 * lectura: copias inmutables, listas no modificables o interfaces sin métodos de modificación.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Planta {

    /** Id de la compañía: su índice en la organización. */
    private final int idCompania;

    /** Nombre visible de la compañía. */
    private final String nombre;

    private final long semilla;

    /** Arreglo dinámico: inventario de máquinas. Se recorre completo en cada tick. */
    private final List<Maquina> inventario;

    /** Cola FIFO: eventos detectados que esperan clasificación. */
    private final Queue<EventoFallo> eventos;

    /**
     * Dos colas FIFO de técnicos disponibles, una por especialidad: en cada una, el primero en
     * esperar es el primero en recibir trabajo. Es un arreglo de colas indexado por una clave fija,
     * como la cola con prioridad: un {@code EnumMap} es por dentro un arreglo indexado por el
     * ordinal del enumerado, así que elegir la cola cuesta O(1).
     */
    private final Map<TipoFallo, Queue<Tecnico>> tecnicosLibres;

    /** Todos los técnicos, libres u ocupados, para que la vista los muestre. */
    private final List<Tecnico> tecnicos;

    /** Cola con prioridad: órdenes de trabajo por atender. */
    private final ColaPrioridadOrdenes ordenes;

    /** Árbol rojo-negro ({@code TreeSet}): máquinas ordenadas por puntaje de fallos. */
    private final RankingFallos ranking;

    /**
     * Conjunto: etiquetas de las máquinas de la compañía, sin repetidos. Se declara con el tipo
     * abstracto {@code Set}; la implementación elegida es {@code TreeSet} (árbol rojo-negro).
     */
    private final Set<String> etiquetas;

    /** Pila: acciones del operador, la última en entrar es la primera en revertirse. */
    private final Deque<ComandoOperador> pilaDeshacer;

    /** Registro de lo que pasó en cada tick; conserva las últimas líneas. */
    private final Deque<String> bitacora;

    private final List<ObservadorPlanta> observadores;

    private GeneradorFallos generador;
    private int tick;
    private int siguienteIdOrden;

    /** Quién dispara los pasos 1, 4 y 6 del tick: el reloj o el jugador. */
    private ModoSimulacion modo;

    /** Caja, ingresos, costos y disponibilidad de la compañía (Entrega 2). */
    private final Contabilidad contabilidad;

    /** Fallos clasificados en la partida (órdenes P1 a P3; el preventivo no es un fallo). */
    private int numeroFallos;

    /**
     * Crea una planta suelta, sin organización: la planta única de la Entrega 1.
     *
     * <p>Es la compañía 0, se llama "Planta" y no tiene ranking global.</p>
     *
     * @param semilla semilla del generador aleatorio; la misma semilla repite la simulación
     */
    public Planta(long semilla) {
        this(0, "Planta", semilla, null, 0);
    }

    /**
     * Crea la planta de una compañía, con su inventario y sus técnicos, lista para el primer tick.
     *
     * @param idCompania    id de la compañía: su índice en la organización
     * @param nombre        nombre visible de la compañía
     * @param semilla       semilla del generador aleatorio; la misma semilla repite la simulación
     * @param rankingGlobal ranking de toda la organización, que esta planta mantiene al día con
     *                      sus máquinas; {@code null} si la planta está sola
     * @param tickInicial   tick con que arranca. Una compañía creada a mitad de la simulación
     *                      arranca en el tick actual de la organización, para que todas las
     *                      plantas tengan siempre el mismo tick
     * @throws NullPointerException     si el nombre es nulo
     * @throws IllegalArgumentException si el tick inicial es negativo
     */
    public Planta(int idCompania, String nombre, long semilla, RankingFallos rankingGlobal,
                  int tickInicial) {
        if (tickInicial < 0) {
            throw new IllegalArgumentException("El tick inicial no puede ser negativo: " + tickInicial);
        }
        this.idCompania = idCompania;
        this.nombre = Objects.requireNonNull(nombre, "La compañía debe tener nombre");
        this.semilla = semilla;
        this.inventario = ConfiguracionPlanta.crearMaquinas(idCompania);
        this.tecnicos = ConfiguracionPlanta.crearTecnicos();
        this.eventos = new ArrayDeque<>();
        this.tecnicosLibres = new EnumMap<>(TipoFallo.class);
        for (TipoFallo especialidad : TipoFallo.values()) {
            tecnicosLibres.put(especialidad, new ArrayDeque<>());
        }
        encolarTecnicosLibres();
        this.ordenes = new ColaPrioridadOrdenes();
        this.ranking = new RankingFallos(inventario, rankingGlobal);
        this.etiquetas = new TreeSet<>();
        for (Maquina maquina : inventario) {
            etiquetas.add(maquina.etiqueta());
        }
        this.pilaDeshacer = new ArrayDeque<>();
        this.bitacora = new ArrayDeque<>();
        this.observadores = new ArrayList<>();
        this.generador = new GeneradorFallos(semilla);
        this.tick = tickInicial;
        this.siguienteIdOrden = 1;
        this.modo = ModoSimulacion.AUTOMATICO;
        this.contabilidad = new Contabilidad(ConfiguracionPlanta.PRESUPUESTO_INICIAL);
        this.numeroFallos = 0;
        registrar("Planta iniciada con semilla " + semilla);
    }

    // ---------------------------------------------------------------------------------
    // Simulación
    // ---------------------------------------------------------------------------------

    /**
     * Avanza la simulación un tick, ejecutando los ocho pasos en orden fijo.
     *
     * <p>El orden importa y no es arbitrario: una anomalía aparece antes de que se lea el sensor
     * que la delata, la detección va antes de la clasificación, y los trabajos avanzan antes de
     * asignar nuevos, para que un técnico que acaba de terminar pueda recibir otra orden en el
     * mismo tick.</p>
     *
     * <p>En {@linkplain ModoSimulacion#MANUAL modo manual} se saltan los pasos 1 (fallos
     * aleatorios), 4 (clasificar) y 6 (asignar): el jugador los dispara con
     * {@link #inyectarFallo}, {@link #clasificarSiguiente()} y
     * {@link #asignarSiguiente(TipoFallo)}.</p>
     *
     * <p>Después de los preventivos se contabiliza el tick (paso 7b). Una compañía en quiebra no
     * opera: solo avanza su tick, para que todas las plantas sigan teniendo el mismo.</p>
     *
     * <p>Al final avisa a los observadores. No es segura para hilos: quien la use debe serializar
     * el acceso, como hace el servicio de simulación con sus métodos {@code synchronized}.</p>
     */
    public void tick() {
        if (contabilidad.enQuiebra()) {
            tick++;
            notificar();
            return;
        }
        if (modo == ModoSimulacion.AUTOMATICO) {
            generarFallos();
        }
        leerSensores();
        detectarAnomalias();
        if (modo == ModoSimulacion.AUTOMATICO) {
            clasificarUnEvento();
        }
        avanzarTrabajos();
        if (modo == ModoSimulacion.AUTOMATICO) {
            asignarTrabajos();
        }
        revisarPreventivos();
        contabilizar();
        tick++;
        notificar();
    }

    /**
     * Paso 1: cada máquina operando y sin anomalía puede desarrollar una.
     *
     * <p>Las máquinas que ya tienen una anomalía no desarrollan otra: el fallo sigue en curso hasta
     * que un técnico lo repara.</p>
     */
    private void generarFallos() {
        for (Maquina maquina : inventario) {
            if (maquina.estado() != EstadoMaquina.OPERANDO || maquina.tieneAnomalia()) {
                continue;
            }
            generador.intentarGenerar(maquina).ifPresent(anomalia -> {
                maquina.activarAnomalia(anomalia);
                registrar(maquina.etiqueta() + ": aparece anomalía "
                        + anomalia.magnitud().descripcion().toLowerCase()
                        + " en " + maquina.sensor(anomalia.indiceSensor()).nombre());
            });
        }
    }

    /**
     * Paso 2: las máquinas que están funcionando generan una lectura y acumulan desgaste.
     *
     * <p>El valor de cada sensor es su nominal más un ruido aleatorio. Si la máquina tiene una
     * anomalía en ese sensor, se le suma además el desplazamiento de la anomalía, y eso es lo que
     * saca la lectura de su rango normal.</p>
     *
     * <p>La lectura se encola en la cola circular de la máquina. Si está llena, la más antigua se
     * pierde: memoria fija, como en un registrador de datos real.</p>
     */
    private void leerSensores() {
        for (Maquina maquina : inventario) {
            if (!maquina.estado().generaLecturas()) {
                continue;
            }
            maquina.envejecer();
            List<Double> valores = new ArrayList<>(maquina.sensores().size());
            for (int indice = 0; indice < maquina.sensores().size(); indice++) {
                Sensor sensor = maquina.sensor(indice);
                double valor = sensor.nominal() + generador.ruidoPara(sensor);
                if (maquina.tieneAnomalia()) {
                    Anomalia anomalia = maquina.anomalia().orElseThrow();
                    if (anomalia.indiceSensor() == indice) {
                        valor += anomalia.desplazamiento();
                    }
                }
                valores.add(valor);
            }
            maquina.registrarLectura(new Lectura(tick, valores));
        }
    }

    /**
     * Paso 3: si la última lectura supera el umbral de advertencia, se genera un evento.
     *
     * <p>Una máquina con alarma activa no genera eventos nuevos, y de cada máquina sale un solo
     * evento por vez. Esa regla es la que evita la inundación de alarmas, que es un problema real
     * en las salas de control y algo que ISA-18.2 busca precisamente prevenir.</p>
     */
    private void detectarAnomalias() {
        for (int indice = 0; indice < inventario.size(); indice++) {
            Maquina maquina = inventario.get(indice);
            if (maquina.alarmaActiva()
                    || !maquina.estado().generaLecturas()
                    || maquina.lecturas().estaVacia()) {
                continue;
            }
            Lectura ultima = maquina.lecturas().verMasReciente();
            for (int s = 0; s < maquina.sensores().size(); s++) {
                Sensor sensor = maquina.sensor(s);
                double valor = ultima.valor(s);
                if (!sensor.esAnomalo(valor)) {
                    continue;
                }
                eventos.add(new EventoFallo(
                        indice, maquina.etiqueta(), s, sensor.nombre(), valor, tick,
                        sensor.tipoFallo()));
                maquina.activarAlarma();
                maquina.cambiarEstado(EstadoMaquina.EN_ALARMA);
                registrar(maquina.etiqueta() + ": " + sensor.nombre() + " en "
                        + sensor.formatear(valor) + " → evento encolado");
                break;
            }
        }
    }

    /**
     * Paso 4: se clasifica <strong>un solo</strong> evento por tick.
     *
     * <p>Clasificar uno a la vez es deliberado: así la cola de eventos se puede acumular y el panel
     * FIFO tiene algo que mostrar. Si se clasificaran todos de golpe, la cola estaría siempre
     * vacía y no se vería su comportamiento.</p>
     *
     * <p>Si la orden resulta P1, la máquina se detiene de inmediato.</p>
     *
     * <p>El fallo se suma al ranking aquí, cuando ocurre, y no cada cierto tiempo: reubicar una
     * máquina cuesta O(log n), mientras que reordenar todo en cada tick costaría O(n log n) aunque
     * no hubiera fallos nuevos.</p>
     *
     * <p>En modo manual lo dispara {@link #clasificarSiguiente()}.</p>
     *
     * @return {@code true} si había un evento y se clasificó; {@code false} si la cola estaba vacía
     */
    private boolean clasificarUnEvento() {
        EventoFallo evento = eventos.poll();
        if (evento == null) {
            return false;
        }
        Maquina maquina = inventario.get(evento.indiceMaquina());
        Sensor sensor = maquina.sensor(evento.indiceSensor());
        OrdenTrabajo orden = Clasificador.clasificar(evento, sensor, siguienteIdOrden++);
        ordenes.insertar(orden);
        numeroFallos++;
        ranking.sumarFallo(maquina, orden.prioridad().peso());
        registrar("Clasificado: " + orden.etiquetaCorta() + " → " + orden.prioridad().etiqueta());

        if (orden.prioridad() == Prioridad.P1) {
            maquina.cambiarEstado(EstadoMaquina.DETENIDA);
            registrar(maquina.etiqueta() + ": paro inmediato por alarma crítica");
        }
        return true;
    }

    /**
     * Paso 5: a cada técnico ocupado se le descuenta un tick.
     *
     * <p>Al terminar, la máquina vuelve a operar sin anomalía ni alarma, y el técnico se encola
     * <strong>al final</strong> de la cola de disponibles, de modo que el trabajo se reparta de
     * forma pareja.</p>
     *
     * <p>El contador de mantenimiento <strong>solo se reinicia si la orden atendida era P4</strong>,
     * es decir un mantenimiento preventivo. Una reparación correctiva arregla el fallo pero no es
     * un servicio completo: cambiar un rodamiento no equivale a la revisión programada, así que el
     * reloj del preventivo sigue corriendo. Sin esta distinción, en una planta con fallos frecuentes
     * cada reparación aplazaba el preventivo y el carril P4 nunca llegaba a usarse.</p>
     */
    private void avanzarTrabajos() {
        for (Tecnico tecnico : tecnicos) {
            if (tecnico.estaLibre()) {
                continue;
            }
            tecnico.avanzar();
            if (!tecnico.trabajoTerminado()) {
                continue;
            }
            OrdenTrabajo terminada = tecnico.liberar();
            Maquina maquina = inventario.get(terminada.indiceMaquina());
            maquina.cambiarEstado(EstadoMaquina.OPERANDO);
            maquina.quitarAnomalia();
            maquina.quitarAlarma();
            if (terminada.prioridad() == Prioridad.P4) {
                maquina.reiniciarMantenimiento();
                maquina.quitarPreventivaPendiente();
            }
            tecnicosLibres.get(tecnico.especialidad()).add(tecnico);
            registrar(tecnico.nombre() + " terminó " + terminada.etiquetaCorta()
                    + "; " + maquina.etiqueta() + " vuelve a operar");
        }
    }

    /**
     * Paso 6: mientras haya trabajo que asignar, se asigna.
     *
     * <p>Sale siempre la orden más prioritaria y entra siempre el técnico de su especialidad que
     * llevaba más tiempo esperando. Aquí se ven trabajando juntas la cola con prioridad y las
     * colas FIFO de técnicos.</p>
     *
     * <p>Si no hay técnico libre de la especialidad que pide la orden siguiente, el paso se detiene
     * aunque haya técnicos libres de la otra: no se salta a la orden de atrás, porque eso sería
     * sacar de la cola con prioridad un elemento que no es el siguiente. Es la política ingenua
     * del modo automático; en modo manual el jugador puede mandar a un técnico de la otra
     * especialidad, pagando la penalidad.</p>
     */
    private void asignarTrabajos() {
        while (asignarUna()) {
            // Cada vuelta asigna una orden; el ciclo termina cuando falta técnico u orden.
        }
    }

    /**
     * Política automática: asigna la orden siguiente a un técnico de su especialidad.
     *
     * <p>Mira la orden siguiente sin extraerla. Si su especialidad no tiene técnicos libres, no
     * asigna nada.</p>
     *
     * @return {@code true} si se asignó; {@code false} si no había orden o técnico de la especialidad
     */
    private boolean asignarUna() {
        descartarPreventivosObsoletos();
        return ordenes.verSiguiente()
                .map(orden -> asignarA(orden.tipoFallo()))
                .orElse(false);
    }

    /**
     * Asigna una sola orden: extrae la más prioritaria y desencola el primer técnico libre de la
     * especialidad indicada.
     *
     * <p>Es la misma operación en los dos modos: el paso 6 la repite con la especialidad que pide
     * cada orden, y en modo manual la dispara {@link #asignarSiguiente(TipoFallo)} con la que elige
     * el jugador. Las estructuras deciden qué sale; el modo solo decide cuándo y a qué cola. Si la
     * especialidad no coincide con el fallo, el técnico tarda más.</p>
     *
     * <p>Complejidad: O(1). Elegir la cola es O(1), extraer recorre los 4 niveles fijos de la cola
     * con prioridad y desencolar es O(1).</p>
     *
     * @param especialidad cola de técnicos de la que sale el técnico
     * @return {@code true} si se asignó; {@code false} si no había orden o técnico libre en esa cola
     */
    private boolean asignarA(TipoFallo especialidad) {
        descartarPreventivosObsoletos();
        Queue<Tecnico> cola = tecnicosLibres.get(especialidad);
        if (cola.isEmpty() || ordenes.estaVacia()) {
            return false;
        }
        OrdenTrabajo orden = ordenes.extraer().orElseThrow();
        Tecnico tecnico = cola.poll();
        tecnico.asignar(orden);
        Maquina maquina = inventario.get(orden.indiceMaquina());
        maquina.cambiarEstado(EstadoMaquina.EN_MANTENIMIENTO);
        String detalle = tecnico.esDeLaEspecialidad(orden)
                ? tecnico.ticksRestantes() + " ticks"
                : "especialidad distinta: " + tecnico.ticksRestantes() + " ticks";
        registrar(tecnico.nombre() + " atiende " + orden.etiquetaCorta()
                + " (" + orden.prioridad().name() + ", " + detalle + ")");
        return true;
    }

    /**
     * Descarte perezoso: mientras la orden del frente sea un preventivo que ya no hace falta, se
     * extrae y se descarta sin asignarla.
     *
     * <p>Un preventivo sobra cuando la máquina recibió un overhaul mientras la orden esperaba,
     * porque el overhaul es un servicio completo. Sacarlo del medio de la cola con prioridad
     * violaría el tipo abstracto de datos, así que se deja en su carril y se descarta cuando llega
     * al frente y sale por la operación normal {@code extraer}. Es la técnica habitual con colas de
     * prioridad (Dijkstra la usa con las distancias viejas).</p>
     *
     * <p>Complejidad: O(1) por orden descartada.</p>
     */
    private void descartarPreventivosObsoletos() {
        while (ordenes.verSiguiente().filter(this::preventivoObsoleto).isPresent()) {
            OrdenTrabajo descartada = ordenes.extraer().orElseThrow();
            registrar(descartada.etiquetaCorta() + " descartada: "
                    + descartada.etiquetaMaquina() + " ya recibió overhaul");
        }
    }

    /** Un preventivo sobra si la máquina ya no lo tiene pendiente o está en overhaul. */
    private boolean preventivoObsoleto(OrdenTrabajo orden) {
        if (orden.prioridad() != Prioridad.P4) {
            return false;
        }
        Maquina maquina = inventario.get(orden.indiceMaquina());
        return !maquina.preventivaPendiente() || maquina.estado() == EstadoMaquina.EN_OVERHAUL;
    }

    /**
     * Inicia el overhaul de una máquina: la deja {@code EN_OVERHAUL}, quita su preventivo pendiente
     * (el overhaul lo reemplaza) y cobra el overhaul a la caja de la compañía. Lo llama la
     * organización al despachar el camión del fabricante.
     *
     * @param indiceMaquina índice de la máquina
     * @param tecnico       nombre del técnico del fabricante, para la bitácora
     */
    void iniciarOverhaul(int indiceMaquina, String tecnico) {
        Maquina maquina = inventario.get(indiceMaquina);
        boolean estabaEnQuiebra = contabilidad.enQuiebra();
        maquina.cambiarEstado(EstadoMaquina.EN_OVERHAUL);
        maquina.quitarAnomalia();
        maquina.quitarPreventivaPendiente();
        contabilidad.cobrar(ConfiguracionPlanta.COSTO_OVERHAUL);
        registrar(tecnico + " inicia el overhaul de " + maquina.etiqueta()
                + " (" + ConfiguracionPlanta.DURACION_OVERHAUL + " ticks, costo "
                + ConfiguracionPlanta.COSTO_OVERHAUL + ")");
        if (!estabaEnQuiebra && contabilidad.enQuiebra()) {
            registrar(nombre + " entra en quiebra: caja " + contabilidad.caja());
        }
        notificar();
    }

    /**
     * Cierra el overhaul de una máquina: el puntaje vuelve a 0, sube el número de overhauls, se
     * reinicia el reloj del preventivo (el overhaul <strong>es</strong> un servicio completo) y la
     * máquina vuelve a operar.
     *
     * <p>El puntaje es la clave de los rankings, así que la máquina sale de los dos árboles antes de
     * cambiarlo y vuelve a entrar después (la trampa de la clave que cambia).</p>
     *
     * <p>Complejidad: O(log N), por los dos rankings.</p>
     *
     * @param indiceMaquina índice de la máquina
     */
    void terminarOverhaul(int indiceMaquina) {
        Maquina maquina = inventario.get(indiceMaquina);
        ranking.quitar(maquina);
        maquina.completarOverhaul();
        ranking.agregar(maquina);
        maquina.reiniciarMantenimiento();
        maquina.quitarAnomalia();
        maquina.quitarAlarma();
        maquina.cambiarEstado(EstadoMaquina.OPERANDO);
        registrar("Overhaul terminado: " + maquina.etiqueta() + " vuelve a operar con puntaje 0 ("
                + maquina.numeroOverhauls() + " overhaul" + (maquina.numeroOverhauls() == 1 ? "" : "s") + ")");
        notificar();
    }

    /**
     * Paso 7b: la economía del tick.
     *
     * <p>Cada máquina que produce (operando o en alarma) ingresa en proporción a su carga, y cada
     * técnico ocupado cobra su mano de obra. Una máquina detenida no tiene un costo aparte: el
     * castigo es que no produce. Si la caja baja de 0, la compañía entra en quiebra.</p>
     *
     * <p>Complejidad: O(n + t), con n las máquinas y t los técnicos.</p>
     */
    private void contabilizar() {
        boolean estabaEnQuiebra = contabilidad.enQuiebra();
        contabilidad.iniciarTick();
        for (Maquina maquina : inventario) {
            boolean produce = maquina.estado() == EstadoMaquina.OPERANDO
                    || maquina.estado() == EstadoMaquina.EN_ALARMA;
            contabilidad.registrarMaquina(produce);
            if (produce) {
                contabilidad.ingresar(ConfiguracionPlanta.INGRESO_POR_TICK * maquina.carga() / 100);
            }
        }
        for (Tecnico tecnico : tecnicos) {
            if (!tecnico.estaLibre()) {
                contabilidad.cobrar(ConfiguracionPlanta.MANO_DE_OBRA_POR_TICK);
            }
        }
        if (!estabaEnQuiebra && contabilidad.enQuiebra()) {
            registrar(nombre + " entra en quiebra: caja " + contabilidad.caja());
        }
    }

    /** Pone a todos los técnicos en la cola de su especialidad, en el orden de la lista. */
    private void encolarTecnicosLibres() {
        for (Tecnico tecnico : tecnicos) {
            tecnicosLibres.get(tecnico.especialidad()).add(tecnico);
        }
    }

    /**
     * Paso 7: las máquinas que cumplieron su intervalo reciben una orden P4.
     *
     * <p>El preventivo no es un fallo, así que no pasa por la cola de eventos: la orden entra
     * directo a la cola con prioridad. El indicador de preventiva pendiente de la máquina evita
     * emitir una orden nueva en cada tick mientras la anterior espera.</p>
     */
    private void revisarPreventivos() {
        for (int indice = 0; indice < inventario.size(); indice++) {
            Maquina maquina = inventario.get(indice);
            if (!maquina.requierePreventivo() || maquina.preventivaPendiente()
                    || maquina.estado() == EstadoMaquina.EN_OVERHAUL) {
                continue;
            }
            OrdenTrabajo orden =
                    Clasificador.ordenPreventiva(maquina, indice, siguienteIdOrden++, tick);
            ordenes.insertar(orden);
            maquina.marcarPreventivaPendiente();
            registrar("Preventivo: " + orden.etiquetaCorta() + " → " + Prioridad.P4.etiqueta());
        }
    }

    /**
     * Devuelve la planta a su estado inicial, con la misma semilla.
     *
     * <p>Como el generador se vuelve a crear con la misma semilla, la simulación repite
     * exactamente la misma secuencia. Es lo que permite ensayar la demostración y repetirla igual
     * durante la presentación.</p>
     */
    public void reiniciar() {
        // Reiniciar deja el puntaje en 0, que es la clave del ranking. Cada máquina sale de los
        // rankings (el de la compañía y el global) antes de que su clave cambie, y vuelve a entrar
        // después. Al revés, el árbol no la encontraría (ver RankingFallos).
        for (Maquina maquina : inventario) {
            ranking.quitar(maquina);
            maquina.reiniciar();
            ranking.agregar(maquina);
        }
        for (Tecnico tecnico : tecnicos) {
            tecnico.reiniciar();
        }
        eventos.clear();
        ordenes.vaciar();
        pilaDeshacer.clear();
        bitacora.clear();
        for (Queue<Tecnico> cola : tecnicosLibres.values()) {
            cola.clear();
        }
        encolarTecnicosLibres();
        generador = new GeneradorFallos(semilla);
        tick = 0;
        siguienteIdOrden = 1;
        contabilidad.reiniciar();
        numeroFallos = 0;
        // El modo no se toca: reiniciar en medio de una demostración en manual sigue en manual.
        registrar("Planta reiniciada con semilla " + semilla + " (modo " + modo.descripcion() + ")");
        notificar();
    }

    // ---------------------------------------------------------------------------------
    // Acciones del operador
    // ---------------------------------------------------------------------------------

    /**
     * Ejecuta una acción del operador y la apila para poder deshacerla.
     *
     * @param comando acción a ejecutar
     * @throws NullPointerException si el comando es nulo
     */
    public void ejecutar(ComandoOperador comando) {
        Objects.requireNonNull(comando, "El comando no puede ser nulo");
        exigirEnOperacion();
        comando.ejecutar();
        pilaDeshacer.push(comando);
        registrar("Operador: " + comando.descripcion());
        notificar();
    }

    /**
     * Revierte la última acción del operador.
     *
     * <p>Si la simulación ya cambió por su cuenta el estado que el comando había dejado, el comando
     * <strong>se descarta</strong> en vez de revertirse, y queda constancia en la bitácora.
     * Revertirlo produciría un estado inconsistente.</p>
     */
    public void deshacer() {
        exigirEnOperacion();
        if (pilaDeshacer.isEmpty()) {
            registrar("No hay acciones para deshacer");
            notificar();
            return;
        }
        ComandoOperador comando = pilaDeshacer.pop();
        if (comando.sePuedeDeshacer()) {
            comando.deshacer();
            registrar("Deshecho: " + comando.descripcion());
        } else {
            registrar("Descartado, la simulación ya cambió el estado: " + comando.descripcion());
        }
        notificar();
    }

    // ---------------------------------------------------------------------------------
    // Modo manual
    // ---------------------------------------------------------------------------------

    /**
     * Cambia quién dispara los pasos 1, 4 y 6 del tick. Se puede hacer con la simulación en marcha.
     *
     * <p>No se apila en la pila de deshacer: es un interruptor de la simulación, no una acción
     * sobre una máquina. Si el modo no cambia, no hace nada.</p>
     *
     * @param nuevo modo nuevo
     * @throws NullPointerException si el modo es nulo
     */
    public void cambiarModo(ModoSimulacion nuevo) {
        Objects.requireNonNull(nuevo, "El modo no puede ser nulo");
        if (nuevo == modo) {
            return;
        }
        modo = nuevo;
        registrar("Modo " + modo.descripcion());
        notificar();
    }

    /**
     * Devuelve el modo actual.
     *
     * @return el modo de la simulación
     */
    public ModoSimulacion modo() {
        return modo;
    }

    /**
     * Botón del modo manual: clasifica el siguiente evento de la cola, el más antiguo.
     *
     * <p>Ejecuta el mismo paso 4 del tick automático. El jugador no elige el evento: la cola FIFO
     * entrega su frente.</p>
     *
     * <p>Complejidad: O(log n) por la actualización del ranking; desencolar e insertar en la cola
     * con prioridad son O(1).</p>
     *
     * @return {@code true} si se clasificó un evento; {@code false} si no había ninguno, y entonces
     *         queda la razón en la bitácora
     * @throws IllegalStateException si la simulación está en modo automático
     */
    public boolean clasificarSiguiente() {
        exigirModoManual();
        exigirEnOperacion();
        boolean clasificado = clasificarUnEvento();
        if (!clasificado) {
            registrar("No hay eventos por clasificar");
        }
        notificar();
        return clasificado;
    }

    /**
     * Botón del modo manual: asigna la orden más prioritaria al primer técnico libre de la
     * especialidad que elige el jugador.
     *
     * <p>Ejecuta la misma operación del paso 6 del tick automático. El jugador elige la
     * <strong>cola</strong>, no el técnico ni la orden: la cola con prioridad y la cola FIFO de esa
     * especialidad entregan su siguiente elemento. Si la especialidad no es la del fallo, la
     * reparación tarda {@link ConfiguracionPlanta#PENALIDAD_ESPECIALIDAD} veces más.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @param especialidad cola de técnicos de la que sale el técnico
     * @return {@code true} si se asignó; {@code false} si no había orden pendiente o técnico libre
     *         de esa especialidad, y entonces queda la razón en la bitácora
     * @throws IllegalStateException si la simulación está en modo automático
     * @throws NullPointerException  si la especialidad es nula
     */
    public boolean asignarSiguiente(TipoFallo especialidad) {
        exigirModoManual();
        exigirEnOperacion();
        Objects.requireNonNull(especialidad, "Falta la especialidad");
        boolean asignada = asignarA(especialidad);
        if (!asignada) {
            registrar(ordenes.estaVacia() ? "No hay órdenes por asignar"
                    : "No hay técnicos " + especialidad.descripcion() + "s libres");
        }
        notificar();
        return asignada;
    }

    private void exigirEnOperacion() {
        if (contabilidad.enQuiebra()) {
            throw new IllegalStateException(nombre + " está en quiebra: no acepta acciones");
        }
    }

    private void exigirModoManual() {
        if (modo != ModoSimulacion.MANUAL) {
            throw new IllegalStateException("La acción solo está disponible en modo manual");
        }
    }

    /**
     * Instala una anomalía a mano en una máquina, para forzar un caso en la demostración.
     *
     * <p>No es deshacible: una vez que la anomalía produce una lectura y esa lectura genera un
     * evento, revertirla obligaría a sacar elementos del medio de las colas, lo que rompe el tipo
     * abstracto de datos.</p>
     *
     * <p>Se rechaza si la máquina ya tiene una anomalía o una alarma activa. Una máquina guarda un
     * solo fallo a la vez: aceptar otro reemplazaría el anterior, y una falla severa podría
     * desaparecer detrás de una leve. Es la misma regla que sigue el generador aleatorio. El
     * rechazo queda en la bitácora.</p>
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @param indiceSensor  sensor que se va a desviar
     * @param magnitud      gravedad del fallo que se quiere provocar
     */
    public void inyectarFallo(int indiceMaquina, int indiceSensor, Magnitud magnitud) {
        exigirEnOperacion();
        Maquina maquina = inventario.get(indiceMaquina);
        if (maquina.estado() == EstadoMaquina.EN_OVERHAUL) {
            throw new IllegalStateException(maquina.etiqueta() + " está en overhaul");
        }
        if (maquina.tieneAnomalia() || maquina.alarmaActiva()) {
            registrar("Inyección rechazada: " + maquina.etiqueta() + " ya tiene un fallo activo");
            notificar();
            return;
        }
        maquina.activarAnomalia(generador.crear(maquina, indiceSensor, magnitud));
        registrar("Inyectado por el operador: anomalía " + magnitud.descripcion().toLowerCase()
                + " en " + maquina.etiqueta() + " / " + maquina.sensor(indiceSensor).nombre());
        notificar();
    }

    /**
     * Indica si ya existe en la compañía una máquina con esa etiqueta.
     *
     * <p>La etiqueta se normaliza antes de buscarla: sin espacios a los lados y en mayúsculas, así
     * que {@code "prensa-01"} y {@code "PRENSA-01"} son la misma.</p>
     *
     * <p>Complejidad: O(log n), una búsqueda en el árbol del conjunto. Con la lista habría que
     * recorrer todo el inventario, O(n).</p>
     *
     * @param etiqueta etiqueta a buscar
     * @return {@code true} si ya hay una máquina con esa etiqueta
     * @throws NullPointerException si la etiqueta es nula
     */
    public boolean existeEtiqueta(String etiqueta) {
        return etiquetas.contains(normalizarEtiqueta(etiqueta));
    }

    /**
     * Crea una máquina nueva del catálogo y la agrega al final del inventario.
     *
     * <p>La máquina toma como índice el tamaño actual del inventario, arranca operando con puntaje
     * 0 y entra al conjunto de etiquetas, al ranking de la compañía y al ranking global. No se
     * puede deshacer: desde el tick siguiente puede fallar y entrar en las colas.</p>
     *
     * <p>Complejidad: O(log n): agregar al inventario es O(1) amortizado, y el conjunto y los dos
     * rankings cuestan O(log n) cada uno.</p>
     *
     * @param tipo     tipo de máquina del catálogo
     * @param etiqueta identificador visible; se guarda sin espacios a los lados y en mayúsculas
     * @return la máquina creada
     * @throws IllegalArgumentException si la etiqueta está vacía o ya existe en la compañía
     * @throws NullPointerException     si el tipo o la etiqueta son nulos
     */
    public Maquina crearMaquina(TipoMaquina tipo, String etiqueta) {
        Objects.requireNonNull(tipo, "La máquina debe tener tipo");
        exigirEnOperacion();
        String normalizada = normalizarEtiqueta(etiqueta);
        if (normalizada.isEmpty()) {
            throw new IllegalArgumentException("La etiqueta de la máquina no puede estar vacía");
        }
        if (etiquetas.contains(normalizada)) {
            throw new IllegalArgumentException("Ya existe una máquina con la etiqueta " + normalizada);
        }
        Maquina maquina = ConfiguracionPlanta.crearMaquina(tipo, idCompania, inventario.size(), normalizada);
        inventario.add(maquina);
        etiquetas.add(normalizada);
        ranking.agregar(maquina);
        registrar("Máquina creada: " + normalizada + " (" + tipo.descripcion() + ")");
        notificar();
        return maquina;
    }

    /** Quita los espacios a los lados y pasa a mayúsculas. */
    private static String normalizarEtiqueta(String etiqueta) {
        return Objects.requireNonNull(etiqueta, "La etiqueta no puede ser nula").strip().toUpperCase();
    }

    // ---------------------------------------------------------------------------------
    // Observadores
    // ---------------------------------------------------------------------------------

    /**
     * Registra a alguien interesado en los cambios de la planta.
     *
     * @param observador quien quiere enterarse
     * @throws NullPointerException si el observador es nulo
     */
    public void agregarObservador(ObservadorPlanta observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador no puede ser nulo"));
    }

    /**
     * Avisa a todos los observadores que el estado cambió.
     */
    private void notificar() {
        for (ObservadorPlanta observador : observadores) {
            observador.plantaCambio(this);
        }
    }

    // ---------------------------------------------------------------------------------
    // Consultas de solo lectura, para la vista
    // ---------------------------------------------------------------------------------

    /**
     * Devuelve el número del tick actual.
     *
     * @return ticks transcurridos desde el inicio
     */
    public int tickActual() {
        return tick;
    }

    /**
     * Devuelve la semilla con que corre la simulación.
     *
     * @return semilla del generador aleatorio
     */
    public long semilla() {
        return semilla;
    }

    /**
     * Devuelve el id de la compañía: su índice en la organización.
     *
     * @return id de la compañía
     */
    public int idCompania() {
        return idCompania;
    }

    /**
     * Devuelve el nombre visible de la compañía.
     *
     * @return nombre de la compañía
     */
    public String nombre() {
        return nombre;
    }

    /**
     * Devuelve el inventario de máquinas, en una lista no modificable.
     *
     * @return las máquinas, en el orden de sus índices
     */
    public List<Maquina> inventario() {
        return Collections.unmodifiableList(inventario);
    }

    /**
     * Devuelve una máquina por su posición en el inventario, en O(1).
     *
     * @param indice posición en el inventario
     * @return la máquina en esa posición
     * @throws IndexOutOfBoundsException si el índice no corresponde a ninguna máquina
     */
    public Maquina maquina(int indice) {
        return inventario.get(indice);
    }

    /**
     * Devuelve las máquinas ordenadas por puntaje de fallos, de la peor a la mejor.
     *
     * <p>Complejidad: O(n), un recorrido en orden del árbol; no se ordena nada.</p>
     *
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> rankingFallos() {
        return ranking.enOrden();
    }

    /**
     * Devuelve las máquinas con puntaje mayor o igual que el umbral, en orden de ranking. Con el
     * umbral de overhaul son las candidatas a overhaul.
     *
     * <p>Complejidad: O(log n + k), con k las máquinas devueltas: una consulta por rango en el
     * árbol, que se detiene en la primera máquina por debajo del umbral.</p>
     *
     * @param umbral puntaje mínimo
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> rankingConPuntajeMinimo(int umbral) {
        return ranking.conPuntajeMinimo(umbral);
    }

    /**
     * Devuelve los eventos que esperan clasificación, del primero al último en salir.
     *
     * @return copia inmutable de la cola de eventos
     */
    public List<EventoFallo> eventosPendientes() {
        return List.copyOf(eventos);
    }

    /**
     * Devuelve todos los técnicos, libres u ocupados.
     *
     * @return lista no modificable de técnicos
     */
    public List<Tecnico> tecnicos() {
        return Collections.unmodifiableList(tecnicos);
    }

    /**
     * Devuelve los técnicos disponibles de una especialidad, del primero al último en recibir
     * trabajo.
     *
     * @param especialidad especialidad de la cola
     * @return copia inmutable de la cola de técnicos libres de esa especialidad
     */
    public List<Tecnico> tecnicosLibres(TipoFallo especialidad) {
        return List.copyOf(tecnicosLibres.get(especialidad));
    }

    /**
     * Devuelve cuántos técnicos están libres, sumando las dos especialidades.
     *
     * <p>Complejidad: O(1): una suma por especialidad.</p>
     *
     * @return número de técnicos libres
     */
    public int totalTecnicosLibres() {
        int total = 0;
        for (Queue<Tecnico> cola : tecnicosLibres.values()) {
            total += cola.size();
        }
        return total;
    }

    /**
     * Devuelve la cola con prioridad, en modo de solo consulta.
     *
     * @return las órdenes pendientes, sin posibilidad de modificarlas
     */
    public ColaPrioridadSoloLectura ordenes() {
        return ordenes;
    }

    /**
     * Devuelve las descripciones de las acciones apiladas, <strong>de la cima hacia abajo</strong>.
     *
     * <p>Se entregan como texto y no como comandos, para que desde la vista no se pueda ejecutar ni
     * revertir nada: deshacer solo ocurre a través de {@link #deshacer()}.</p>
     *
     * @return copia inmutable de la pila, la acción más reciente primero
     */
    public List<String> accionesDeshacibles() {
        List<String> descripciones = new ArrayList<>(pilaDeshacer.size());
        for (ComandoOperador comando : pilaDeshacer) {
            descripciones.add(comando.descripcion());
        }
        return Collections.unmodifiableList(descripciones);
    }

    /**
     * Devuelve la contabilidad de la compañía, de solo lectura para quien está fuera del paquete:
     * caja, ingresos, costos y disponibilidad.
     *
     * <p>Tiene visibilidad de paquete para que solo el modelo y sus pruebas puedan mover la caja,
     * como {@code Maquina.sumarPuntaje}.</p>
     *
     * @return la contabilidad
     */
    Contabilidad contabilidad() {
        return contabilidad;
    }

    /**
     * Devuelve el dinero disponible de la compañía.
     *
     * @return la caja; negativa si está en quiebra
     */
    public long caja() {
        return contabilidad.caja();
    }

    /**
     * Devuelve todo lo ingresado en la partida.
     *
     * @return ingresos acumulados
     */
    public long ingresos() {
        return contabilidad.ingresos();
    }

    /**
     * Devuelve todo lo gastado en la partida.
     *
     * @return costos acumulados
     */
    public long costos() {
        return contabilidad.costos();
    }

    /**
     * Devuelve lo gastado en el último tick. Es lo que guardará el historial.
     *
     * @return costo del tick
     */
    public long costoDelTick() {
        return contabilidad.costoDelTick();
    }

    /**
     * Devuelve la fracción del tiempo en que las máquinas produjeron en la partida.
     *
     * @return disponibilidad entre 0 y 1
     */
    public double disponibilidad() {
        return contabilidad.disponibilidad();
    }

    /**
     * Indica si la compañía está en quiebra: no opera ni acepta acciones.
     *
     * @return {@code true} si la caja es negativa
     */
    public boolean enQuiebra() {
        return contabilidad.enQuiebra();
    }

    /**
     * Devuelve cuántos fallos se han clasificado en la partida (órdenes P1 a P3).
     *
     * @return número de fallos
     */
    public int numeroFallos() {
        return numeroFallos;
    }

    /**
     * Devuelve las líneas de la bitácora, de la más antigua a la más reciente.
     *
     * @return copia inmutable de la bitácora
     */
    public List<String> bitacora() {
        return List.copyOf(bitacora);
    }

    /**
     * Deja una línea en la bitácora desde otra clase del modelo, por ejemplo cuando la organización
     * aprueba un overhaul de esta compañía.
     *
     * @param mensaje texto de la línea, sin el tick
     */
    void anotar(String mensaje) {
        registrar(mensaje);
        notificar();
    }

    /**
     * Agrega una línea a la bitácora, descartando las más viejas si se pasa del máximo.
     *
     * @param mensaje qué pasó
     */
    private void registrar(String mensaje) {
        bitacora.addLast("Tick " + tick + ": " + mensaje);
        while (bitacora.size() > ConfiguracionPlanta.MAX_LINEAS_BITACORA) {
            bitacora.removeFirst();
        }
    }
}
