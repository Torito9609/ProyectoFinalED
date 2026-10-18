package edu.unal.planta.modelo;

import java.util.Objects;

/**
 * Detección cruda de una lectura anómala, <strong>sin prioridad todavía</strong>.
 *
 * <p>Es lo que se encola en la cola FIFO de eventos por clasificar. La falta de prioridad es lo
 * importante: así funciona en la industria, donde el PLC solo detecta que una variable se salió de
 * rango, el SCADA la clasifica y el sistema de mantenimiento emite la orden. Esa separación es la
 * que justifica que el proyecto tenga dos colas distintas, una FIFO y una con prioridad.</p>
 *
 * <p>Como {@link OrdenTrabajo}, guarda el índice de la máquina para recuperarla del inventario en
 * O(1), y los nombres para que el panel y la bitácora puedan mostrar el evento sin consultar nada
 * más.</p>
 *
 * @param indiceMaquina   posición de la máquina en el inventario
 * @param etiquetaMaquina etiqueta de la máquina, por ejemplo {@code "PRENSA-01"}
 * @param indiceSensor    posición del sensor en la lista de sensores de la máquina
 * @param nombreSensor    nombre del sensor que se salió de rango
 * @param valorLeido      valor que disparó la detección
 * @param tick            tick en que se detectó
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EventoFallo(
        int indiceMaquina,
        String etiquetaMaquina,
        int indiceSensor,
        String nombreSensor,
        double valorLeido,
        int tick) {

    /**
     * Valida el evento.
     *
     * @throws IllegalArgumentException si algún índice o el tick son negativos
     * @throws NullPointerException     si la etiqueta o el nombre del sensor son nulos
     */
    public EventoFallo {
        Objects.requireNonNull(etiquetaMaquina, "El evento debe indicar la máquina");
        Objects.requireNonNull(nombreSensor, "El evento debe indicar el sensor");
        if (indiceMaquina < 0 || indiceSensor < 0 || tick < 0) {
            throw new IllegalArgumentException("Índices y tick no pueden ser negativos");
        }
    }
}
