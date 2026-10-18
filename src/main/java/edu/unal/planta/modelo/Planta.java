package edu.unal.planta.modelo;

import edu.unal.planta.modelo.comandos.ComandoOperador;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Fachada del modelo: contiene las cinco estructuras de datos y ejecuta la simulación.
 *
 * <h2>Las cinco estructuras</h2>
 * <table border="1">
 *   <caption>Estructuras que viven en esta clase</caption>
 *   <tr><th>Campo</th><th>Estructura</th><th>Para qué</th></tr>
 *   <tr><td>{@code inventario}</td><td>{@code ArrayList}</td><td>Las máquinas de la planta</td></tr>
 *   <tr><td>{@code eventos}</td><td>{@code ArrayDeque} como {@code Queue}</td>
 *       <td>Eventos por clasificar, en orden de llegada</td></tr>
 *   <tr><td>{@code tecnicosLibres}</td><td>{@code ArrayDeque} como {@code Queue}</td>
 *       <td>Técnicos disponibles, en orden de llegada</td></tr>
 *   <tr><td>{@code ordenes}</td><td>{@link ColaPrioridadOrdenes}</td>
 *       <td>Órdenes de trabajo P1 a P4</td></tr>
 *   <tr><td>{@code pilaDeshacer}</td><td>{@code ArrayDeque} como pila</td>
 *       <td>Acciones del operador que se pueden revertir</td></tr>
 * </table>
 *
 * <p>La cola circular no aparece aquí porque cada {@link Maquina} es dueña de la suya, con sus
 * propias lecturas. La bitácora usa también un {@code ArrayDeque}, pero es un registro auxiliar y
 * no una de las cinco estructuras del enunciado: se eligió deque porque al llenarse hay que
 * descartar las líneas más viejas por el frente, en O(1).</p>
 *
 * <h2>Reglas de arquitectura</h2>
 * <p>Esta clase no importa nada de Swing ni de AWT. Cuando el estado cambia, avisa a sus
 * {@link ObservadorPlanta} y no sabe quién escucha. Todo lo que entrega a la vista es de solo
 * lectura: copias inmutables, listas no modificables o interfaces sin métodos de modificación.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Planta {

    private final long semilla;

    /** Arreglo dinámico: inventario de máquinas. Se recorre completo en cada tick. */
    private final List<Maquina> inventario;

    /** Cola FIFO: eventos detectados que esperan clasificación. */
    private final Queue<EventoFallo> eventos;

    /** Cola FIFO: técnicos disponibles, el primero en esperar es el primero en recibir trabajo. */
    private final Queue<Tecnico> tecnicosLibres;

    /** Todos los técnicos, libres u ocupados, para que la vista los muestre. */
    private final List<Tecnico> tecnicos;

    /** Cola con prioridad: órdenes de trabajo por atender. */
    private final ColaPrioridadOrdenes ordenes;

    /** Pila: acciones del operador, la última en entrar es la primera en revertirse. */
    private final Deque<ComandoOperador> pilaDeshacer;

    /** Registro de lo que pasó en cada tick; conserva las últimas líneas. */
    private final Deque<String> bitacora;

    private final List<ObservadorPlanta> observadores;

    private GeneradorFallos generador;
    private int tick;
    private int siguienteIdOrden;

    /**
     * Crea la planta con su inventario y sus técnicos, lista para el primer tick.
     *
     * @param semilla semilla del generador aleatorio; la misma semilla repite la simulación
     */
    public Planta(long semilla) {
        this.semilla = semilla;
        this.inventario = ConfiguracionPlanta.crearMaquinas();
        this.tecnicos = ConfiguracionPlanta.crearTecnicos();
        this.eventos = new ArrayDeque<>();
        this.tecnicosLibres = new ArrayDeque<>(tecnicos);
        this.ordenes = new ColaPrioridadOrdenes();
        this.pilaDeshacer = new ArrayDeque<>();
        this.bitacora = new ArrayDeque<>();
        this.observadores = new ArrayList<>();
        this.generador = new GeneradorFallos(semilla);
        this.tick = 0;
        this.siguienteIdOrden = 1;
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
     * <p>Al final avisa a los observadores. Se ejecuta en el hilo de eventos de Swing, así que no
     * hay concurrencia que manejar.</p>
     */
    public void tick() {
        generarFallos();
        leerSensores();
        detectarAnomalias();
        clasificarUnEvento();
        avanzarTrabajos();
        asignarTrabajos();
        revisarPreventivos();
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
                        indice, maquina.etiqueta(), s, sensor.nombre(), valor, tick));
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
     */
    private void clasificarUnEvento() {
        EventoFallo evento = eventos.poll();
        if (evento == null) {
            return;
        }
        Maquina maquina = inventario.get(evento.indiceMaquina());
        Sensor sensor = maquina.sensor(evento.indiceSensor());
        OrdenTrabajo orden = Clasificador.clasificar(evento, sensor, siguienteIdOrden++);
        ordenes.insertar(orden);
        registrar("Clasificado: " + orden.etiquetaCorta() + " → " + orden.prioridad().etiqueta());

        if (orden.prioridad() == Prioridad.P1) {
            maquina.cambiarEstado(EstadoMaquina.DETENIDA);
            registrar(maquina.etiqueta() + ": paro inmediato por alarma crítica");
        }
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
            tecnicosLibres.add(tecnico);
            registrar(tecnico.nombre() + " terminó " + terminada.etiquetaCorta()
                    + "; " + maquina.etiqueta() + " vuelve a operar");
        }
    }

    /**
     * Paso 6: mientras haya técnicos libres y órdenes pendientes, se asigna trabajo.
     *
     * <p>Sale siempre la orden más prioritaria y entra siempre el técnico que llevaba más tiempo
     * esperando. Aquí se ven trabajando juntas la cola con prioridad y la cola FIFO.</p>
     */
    private void asignarTrabajos() {
        while (!tecnicosLibres.isEmpty() && !ordenes.estaVacia()) {
            OrdenTrabajo orden = ordenes.extraer().orElseThrow();
            Tecnico tecnico = tecnicosLibres.poll();
            tecnico.asignar(orden);
            Maquina maquina = inventario.get(orden.indiceMaquina());
            maquina.cambiarEstado(EstadoMaquina.EN_MANTENIMIENTO);
            registrar(tecnico.nombre() + " atiende " + orden.etiquetaCorta()
                    + " (" + orden.prioridad().name() + ", " + orden.duracionEstimadaTicks() + " ticks)");
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
            if (!maquina.requierePreventivo() || maquina.preventivaPendiente()) {
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
        for (Maquina maquina : inventario) {
            maquina.reiniciar();
        }
        for (Tecnico tecnico : tecnicos) {
            tecnico.reiniciar();
        }
        eventos.clear();
        ordenes.vaciar();
        pilaDeshacer.clear();
        bitacora.clear();
        tecnicosLibres.clear();
        tecnicosLibres.addAll(tecnicos);
        generador = new GeneradorFallos(semilla);
        tick = 0;
        siguienteIdOrden = 1;
        registrar("Planta reiniciada con semilla " + semilla);
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

    /**
     * Instala una anomalía a mano en una máquina, para forzar un caso en la demostración.
     *
     * <p>No es deshacible: una vez que la anomalía produce una lectura y esa lectura genera un
     * evento, revertirla obligaría a sacar elementos del medio de las colas, lo que rompe el tipo
     * abstracto de datos.</p>
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @param indiceSensor  sensor que se va a desviar
     * @param magnitud      gravedad del fallo que se quiere provocar
     */
    public void inyectarFallo(int indiceMaquina, int indiceSensor, Magnitud magnitud) {
        Maquina maquina = inventario.get(indiceMaquina);
        maquina.activarAnomalia(generador.crear(maquina, indiceSensor, magnitud));
        registrar("Inyectado por el operador: anomalía " + magnitud.descripcion().toLowerCase()
                + " en " + maquina.etiqueta() + " / " + maquina.sensor(indiceSensor).nombre());
        notificar();
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
     * Devuelve los técnicos disponibles, del primero al último en recibir trabajo.
     *
     * @return copia inmutable de la cola de técnicos libres
     */
    public List<Tecnico> tecnicosLibres() {
        return List.copyOf(tecnicosLibres);
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
     * Devuelve las líneas de la bitácora, de la más antigua a la más reciente.
     *
     * @return copia inmutable de la bitácora
     */
    public List<String> bitacora() {
        return List.copyOf(bitacora);
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
