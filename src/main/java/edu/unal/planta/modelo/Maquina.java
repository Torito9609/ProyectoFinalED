package edu.unal.planta.modelo;

import edu.unal.planta.estructuras.ColaCircular;
import edu.unal.planta.estructuras.ColaCircularSoloLectura;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una máquina de la planta: sus parámetros fijos y su estado actual.
 *
 * <p>Es la única entidad del modelo con estado mutable, porque es lo que la simulación cambia en
 * cada tick. Los parámetros de configuración (etiqueta, tipo, sensores, intervalo preventivo, tasa
 * de fallo) son inmutables; lo que cambia es el estado, la carga, los ticks desde el último
 * mantenimiento, la anomalía activa y el indicador de alarma.</p>
 *
 * <p>Cada máquina es dueña de su propia {@link ColaCircular} de lecturas, con capacidad fija. Una
 * máquina nunca se elimina del inventario: solo cambia de estado.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Maquina {

    private final String etiqueta;
    private final String tipo;
    private final List<Sensor> sensores;
    private final int intervaloPreventivo;
    private final double tasaBaseFallo;
    private final int cargaInicial;
    private final ColaCircular<Lectura> lecturas;

    private EstadoMaquina estado;
    private int carga;
    private int ticksDesdeMantenimiento;
    private Anomalia anomalia;
    private boolean alarmaActiva;
    private boolean preventivaPendiente;

    /**
     * Crea una máquina lista para operar.
     *
     * @param etiqueta            identificador visible, por ejemplo {@code "PRENSA-01"}
     * @param tipo                descripción del equipo, por ejemplo {@code "Prensa hidráulica"}
     * @param sensores            variables que mide; al menos una
     * @param intervaloPreventivo cada cuántos ticks le toca mantenimiento preventivo
     * @param tasaBaseFallo       probabilidad base de desarrollar una anomalía por tick
     * @param cargaInicial        porcentaje de carga de trabajo inicial, entre 0 y 100
     * @param capacidadLecturas   cuántas lecturas recientes conserva
     * @throws IllegalArgumentException si algún parámetro está fuera de rango
     * @throws NullPointerException     si la etiqueta, el tipo o los sensores son nulos
     */
    public Maquina(String etiqueta, String tipo, List<Sensor> sensores, int intervaloPreventivo,
                   double tasaBaseFallo, int cargaInicial, int capacidadLecturas) {
        this.etiqueta = Objects.requireNonNull(etiqueta, "La máquina debe tener etiqueta");
        this.tipo = Objects.requireNonNull(tipo, "La máquina debe tener tipo");
        this.sensores = List.copyOf(Objects.requireNonNull(sensores, "La máquina debe tener sensores"));
        if (this.sensores.isEmpty()) {
            throw new IllegalArgumentException("La máquina " + etiqueta + " debe tener al menos un sensor");
        }
        if (intervaloPreventivo <= 0) {
            throw new IllegalArgumentException(
                    "El intervalo preventivo debe ser positivo: " + intervaloPreventivo);
        }
        if (tasaBaseFallo < 0 || tasaBaseFallo > 1) {
            throw new IllegalArgumentException(
                    "La tasa base de fallo debe estar entre 0 y 1: " + tasaBaseFallo);
        }
        this.intervaloPreventivo = intervaloPreventivo;
        this.tasaBaseFallo = tasaBaseFallo;
        this.cargaInicial = validarCarga(cargaInicial);
        this.lecturas = new ColaCircular<>(capacidadLecturas);

        this.estado = EstadoMaquina.OPERANDO;
        this.carga = this.cargaInicial;
        this.ticksDesdeMantenimiento = 0;
        this.anomalia = null;
        this.alarmaActiva = false;
        this.preventivaPendiente = false;
    }

    /**
     * Devuelve el identificador visible de la máquina.
     *
     * @return etiqueta, por ejemplo {@code "PRENSA-01"}
     */
    public String etiqueta() {
        return etiqueta;
    }

    /**
     * Devuelve la descripción del equipo.
     *
     * @return tipo de máquina
     */
    public String tipo() {
        return tipo;
    }

    /**
     * Devuelve los sensores de la máquina, en una lista no modificable.
     *
     * <p>El orden de esta lista es el mismo de los valores de cada {@link Lectura}.</p>
     *
     * @return sensores de la máquina
     */
    public List<Sensor> sensores() {
        return sensores;
    }

    /**
     * Devuelve un sensor por su posición.
     *
     * @param indice posición del sensor
     * @return el sensor en esa posición
     * @throws IndexOutOfBoundsException si el índice no corresponde a ningún sensor
     */
    public Sensor sensor(int indice) {
        return sensores.get(indice);
    }

    /**
     * Devuelve la cola de lecturas recientes, como estructura de <strong>solo consulta</strong>.
     *
     * <p>El tipo de retorno es {@link ColaCircularSoloLectura} y no {@link ColaCircular}: quien
     * reciba esta cola puede consultar capacidad, tamaño, punteros, contenido de cada celda y
     * sobrescrituras, pero no tiene manera de encolar ni desencolar. Así la regla de que la vista
     * solo lee el modelo la hace cumplir el compilador y no la buena voluntad del programador.</p>
     *
     * <p>La única forma de agregar una lectura es {@link #registrarLectura(Lectura)}, que es parte
     * de la simulación.</p>
     *
     * @return la cola de lecturas de esta máquina, en modo de solo consulta
     */
    public ColaCircularSoloLectura<Lectura> lecturas() {
        return lecturas;
    }

    /**
     * Devuelve el estado actual de la máquina.
     *
     * @return estado actual
     */
    public EstadoMaquina estado() {
        return estado;
    }

    /**
     * Cambia el estado de la máquina.
     *
     * @param nuevo estado al que pasa
     * @throws NullPointerException si el estado es nulo
     */
    public void cambiarEstado(EstadoMaquina nuevo) {
        this.estado = Objects.requireNonNull(nuevo, "El estado no puede ser nulo");
    }

    /**
     * Devuelve la carga de trabajo actual.
     *
     * @return porcentaje entre 0 y 100
     */
    public int carga() {
        return carga;
    }

    /**
     * Cambia la carga de trabajo, que influye en la probabilidad de fallo.
     *
     * @param nueva porcentaje entre 0 y 100
     * @throws IllegalArgumentException si el porcentaje está fuera de rango
     */
    public void cambiarCarga(int nueva) {
        this.carga = validarCarga(nueva);
    }

    /**
     * Devuelve cuántos ticks pasaron desde el último mantenimiento.
     *
     * @return ticks acumulados
     */
    public int ticksDesdeMantenimiento() {
        return ticksDesdeMantenimiento;
    }

    /**
     * Devuelve cada cuántos ticks corresponde mantenimiento preventivo.
     *
     * @return intervalo preventivo en ticks
     */
    public int intervaloPreventivo() {
        return intervaloPreventivo;
    }

    /**
     * Suma un tick al contador de desgaste desde el último mantenimiento.
     */
    public void envejecer() {
        ticksDesdeMantenimiento++;
    }

    /**
     * Reinicia el contador de desgaste, como si acabara de recibir mantenimiento.
     */
    public void reiniciarMantenimiento() {
        ticksDesdeMantenimiento = 0;
    }

    /**
     * Indica si ya cumplió su intervalo de mantenimiento preventivo.
     *
     * @return {@code true} si corresponde generar una orden P4
     */
    public boolean requierePreventivo() {
        return ticksDesdeMantenimiento >= intervaloPreventivo;
    }

    /**
     * Calcula la probabilidad de que la máquina desarrolle una anomalía en este tick.
     *
     * <p>Aplica la fórmula del documento de diseño:
     * {@code tasaBase × (carga / 100) × (1 + ticksDesdeMantenimiento / intervaloPreventivo)}.
     * Es decir: una máquina más cargada y con el mantenimiento más atrasado falla más seguido, que
     * es el comportamiento que se quiere mostrar en la demostración.</p>
     *
     * <p>El cálculo no tiene aleatoriedad; el sorteo lo hace el generador de fallos.</p>
     *
     * @return probabilidad entre 0 y 1
     */
    public double probabilidadFallo() {
        double factorCarga = carga / 100.0;
        double factorDesgaste = 1.0 + (double) ticksDesdeMantenimiento / intervaloPreventivo;
        return Math.min(1.0, tasaBaseFallo * factorCarga * factorDesgaste);
    }

    /**
     * Devuelve la tasa base de fallo configurada.
     *
     * @return probabilidad base por tick
     */
    public double tasaBaseFallo() {
        return tasaBaseFallo;
    }

    /**
     * Devuelve la anomalía activa, si hay alguna.
     *
     * @return la anomalía en curso, o un {@link Optional} vacío si la máquina está sana
     */
    public Optional<Anomalia> anomalia() {
        return Optional.ofNullable(anomalia);
    }

    /**
     * Indica si la máquina tiene un fallo físico en curso.
     *
     * @return {@code true} si hay una anomalía activa
     */
    public boolean tieneAnomalia() {
        return anomalia != null;
    }

    /**
     * Instala una anomalía en la máquina.
     *
     * @param nueva anomalía que empieza
     * @throws NullPointerException si la anomalía es nula
     */
    public void activarAnomalia(Anomalia nueva) {
        this.anomalia = Objects.requireNonNull(nueva, "La anomalía no puede ser nula");
    }

    /**
     * Quita la anomalía activa, porque un técnico terminó la reparación.
     */
    public void quitarAnomalia() {
        this.anomalia = null;
    }

    /**
     * Indica si la máquina tiene una alarma sin atender.
     *
     * <p>Es un dato aparte del estado, porque una máquina puede estar DETENIDA o
     * EN_MANTENIMIENTO y seguir teniendo su alarma pendiente. Mientras la alarma esté activa no se
     * generan eventos nuevos para esa máquina, y es así como se evita la inundación de alarmas.</p>
     *
     * @return {@code true} si hay una alarma activa
     */
    public boolean alarmaActiva() {
        return alarmaActiva;
    }

    /**
     * Marca que la máquina tiene una alarma sin atender.
     */
    public void activarAlarma() {
        this.alarmaActiva = true;
    }

    /**
     * Quita la alarma, porque la orden correspondiente se atendió.
     */
    public void quitarAlarma() {
        this.alarmaActiva = false;
    }

    /**
     * Indica si la máquina ya tiene una orden de mantenimiento preventivo esperando.
     *
     * <p>Este indicador evita que el paso del preventivo genere una orden P4 nueva en cada tick
     * mientras la anterior sigue sin atenderse. La alternativa era recorrer el carril P4 de la cola
     * con prioridad buscando una orden de esta máquina, que cuesta O(n) y obliga a inspeccionar la
     * cola desde afuera; este indicador se consulta en O(1) y respeta el tipo abstracto de
     * datos.</p>
     *
     * @return {@code true} si hay una orden preventiva pendiente
     */
    public boolean preventivaPendiente() {
        return preventivaPendiente;
    }

    /**
     * Marca que se emitió una orden de mantenimiento preventivo para esta máquina.
     */
    public void marcarPreventivaPendiente() {
        this.preventivaPendiente = true;
    }

    /**
     * Quita la marca de preventivo pendiente, porque la orden ya se atendió.
     */
    public void quitarPreventivaPendiente() {
        this.preventivaPendiente = false;
    }

    /**
     * Agrega una lectura al historial reciente.
     *
     * <p>Si la cola circular está llena, la lectura más antigua se descarta.</p>
     *
     * @param lectura medición del tick actual
     * @throws NullPointerException si la lectura es nula
     */
    public void registrarLectura(Lectura lectura) {
        lecturas.encolar(Objects.requireNonNull(lectura, "La lectura no puede ser nula"));
    }

    /**
     * Devuelve la máquina a su estado inicial, para la acción "Reiniciar" de la simulación.
     */
    public void reiniciar() {
        estado = EstadoMaquina.OPERANDO;
        carga = cargaInicial;
        ticksDesdeMantenimiento = 0;
        anomalia = null;
        alarmaActiva = false;
        preventivaPendiente = false;
        lecturas.vaciar();
    }

    /**
     * Devuelve la etiqueta y el estado, para la bitácora y la depuración.
     *
     * @return texto con la forma {@code "PRENSA-01 (Operando)"}
     */
    @Override
    public String toString() {
        return etiqueta + " (" + estado.descripcion() + ")";
    }

    /**
     * Comprueba que un porcentaje de carga esté entre 0 y 100.
     *
     * @param valor porcentaje a validar
     * @return el mismo valor, si es válido
     * @throws IllegalArgumentException si está fuera de rango
     */
    private static int validarCarga(int valor) {
        if (valor < 0 || valor > 100) {
            throw new IllegalArgumentException("La carga debe estar entre 0 y 100: " + valor);
        }
        return valor;
    }
}
