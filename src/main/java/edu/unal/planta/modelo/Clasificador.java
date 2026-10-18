package edu.unal.planta.modelo;

import java.util.Objects;

/**
 * Convierte un {@link EventoFallo} detectado en una {@link OrdenTrabajo} con prioridad.
 *
 * <p><strong>El clasificador no sabe nada de anomalías.</strong> No consulta la magnitud del fallo,
 * ni si la máquina tiene una anomalía activa, ni los pesos del generador. Lo único que mira es el
 * número que llegó del sensor y lo compara contra los tres umbrales. Por eso funcionaría igual
 * conectado a un sensor real, donde nadie sabe de antemano qué tan grave es el fallo: esa es la
 * prueba de que la clasificación no está haciendo trampa.</p>
 *
 * <table border="1">
 *   <caption>Reglas de clasificación</caption>
 *   <tr><th>Valor leído</th><th>Prioridad</th></tr>
 *   <tr><td>≥ umbral crítico</td><td>P1 CRÍTICA</td></tr>
 *   <tr><td>≥ umbral alto</td><td>P2 ALTA</td></tr>
 *   <tr><td>≥ umbral de advertencia</td><td>P3 MEDIA</td></tr>
 *   <tr><td>Intervalo preventivo cumplido</td><td>P4 BAJA</td></tr>
 * </table>
 *
 * <p>La P4 no viene de un evento, porque el mantenimiento preventivo no es un fallo: la máquina
 * está sana y simplemente le toca revisión. Por eso tiene su propio método y no pasa por la cola de
 * eventos.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Clasificador {

    /** Clase de utilidades: no se instancia. */
    private Clasificador() {
    }

    /**
     * Clasifica un evento comparando su valor contra los umbrales del sensor.
     *
     * @param evento evento detectado
     * @param sensor sensor que lo produjo, para conocer sus umbrales
     * @param id     identificador que se le asigna a la orden
     * @return la orden de trabajo ya priorizada
     * @throws NullPointerException     si el evento o el sensor son nulos
     * @throws IllegalArgumentException si el valor del evento no alcanza el umbral de advertencia,
     *                                 porque entonces no debería haberse detectado
     */
    public static OrdenTrabajo clasificar(EventoFallo evento, Sensor sensor, int id) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        Objects.requireNonNull(sensor, "El sensor no puede ser nulo");

        Prioridad prioridad = prioridadDe(evento.valorLeido(), sensor);
        String descripcion = evento.nombreSensor() + " en "
                + sensor.formatear(evento.valorLeido()) + " (" + prioridad.accion() + ")";

        return new OrdenTrabajo(
                id,
                evento.indiceMaquina(),
                evento.etiquetaMaquina(),
                prioridad,
                descripcion,
                evento.tick(),
                ConfiguracionPlanta.duracionReparacion(prioridad));
    }

    /**
     * Crea la orden de mantenimiento preventivo de una máquina, que siempre es P4.
     *
     * @param maquina       máquina que cumplió su intervalo
     * @param indiceMaquina posición de la máquina en el inventario
     * @param id            identificador que se le asigna a la orden
     * @param tick          tick en que se emite
     * @return la orden preventiva
     * @throws NullPointerException si la máquina es nula
     */
    public static OrdenTrabajo ordenPreventiva(Maquina maquina, int indiceMaquina, int id, int tick) {
        Objects.requireNonNull(maquina, "La máquina no puede ser nula");
        return new OrdenTrabajo(
                id,
                indiceMaquina,
                maquina.etiqueta(),
                Prioridad.P4,
                "Mantenimiento preventivo: " + maquina.ticksDesdeMantenimiento()
                        + " ticks desde el último",
                tick,
                ConfiguracionPlanta.duracionReparacion(Prioridad.P4));
    }

    /**
     * Decide la prioridad de un valor leído según los umbrales del sensor.
     *
     * @param valor  valor leído
     * @param sensor sensor que lo produjo
     * @return la prioridad correspondiente
     * @throws IllegalArgumentException si el valor no alcanza el umbral de advertencia
     */
    private static Prioridad prioridadDe(double valor, Sensor sensor) {
        if (valor >= sensor.umbralCritico()) {
            return Prioridad.P1;
        }
        if (valor >= sensor.umbralAlto()) {
            return Prioridad.P2;
        }
        if (valor >= sensor.umbralAdvertencia()) {
            return Prioridad.P3;
        }
        throw new IllegalArgumentException("El valor " + valor + " de " + sensor.nombre()
                + " no alcanza el umbral de advertencia " + sensor.umbralAdvertencia()
                + ", así que no debió generar un evento");
    }
}
