package edu.unal.planta.modelo;

import java.util.Objects;

/**
 * Orden de trabajo: un evento de fallo ya clasificado, listo para asignarse a un técnico.
 *
 * <p>Se separa a propósito de {@code EventoFallo}, que es la detección cruda y todavía no tiene
 * prioridad. Esa separación refleja cómo funciona la industria: el PLC detecta, el SCADA clasifica
 * y el sistema de mantenimiento genera la orden. Es también lo que justifica que el proyecto tenga
 * dos colas distintas, una FIFO para lo detectado y una con prioridad para lo clasificado.</p>
 *
 * <h2>Por qué es inmutable</h2>
 * <p>Se implementa como {@code record} porque una orden de trabajo es un documento histórico: una
 * vez emitida no cambia. Lo que cambia es su ubicación (en la cola, asignada a un técnico,
 * terminada), y eso lo manejan las estructuras y el técnico, no la orden. Al ser inmutable se puede
 * entregar a la vista sin riesgo de que alguien la modifique.</p>
 *
 * <h2>Por qué guarda el índice de la máquina y no la máquina</h2>
 * <p>La orden no tiene una referencia a {@code Maquina}, sino la posición de esa máquina en el
 * inventario, más su etiqueta:</p>
 * <ul>
 *   <li>El <strong>índice</strong> permite recuperar la máquina del {@code ArrayList} del inventario
 *       en O(1) cuando hay que cambiarle el estado. Es exactamente la razón por la que el inventario
 *       es un arreglo dinámico y no una lista enlazada.</li>
 *   <li>La <strong>etiqueta</strong> permite que la vista dibuje la orden sin tener que resolver la
 *       máquina, y deja la orden legible por sí sola en la bitácora.</li>
 *   <li>Además evita una referencia circular entre máquina y orden, que complicaría tanto la
 *       inmutabilidad como la lectura del código.</li>
 * </ul>
 *
 * @param id                   identificador consecutivo de la orden, empezando en 1
 * @param indiceMaquina        posición de la máquina en el inventario
 * @param etiquetaMaquina      etiqueta de la máquina, por ejemplo {@code "PRENSA-01"}
 * @param prioridad            prioridad asignada por el clasificador
 * @param descripcion          motivo de la orden, legible para el operador
 * @param tickCreacion         tick de la simulación en que se creó
 * @param duracionEstimadaTicks cuántos ticks tarda un técnico en atenderla; debe ser positiva
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record OrdenTrabajo(
        int id,
        int indiceMaquina,
        String etiquetaMaquina,
        Prioridad prioridad,
        String descripcion,
        int tickCreacion,
        int duracionEstimadaTicks) {

    /**
     * Valida los datos de la orden al construirla.
     *
     * <p>Una orden mal formada es un error de programación, no una situación que el operador pueda
     * provocar; por eso se rechaza de inmediato en vez de arrastrar el problema hasta la vista.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @throws IllegalArgumentException si algún número está fuera de rango
     * @throws NullPointerException     si la prioridad, la etiqueta o la descripción son nulas
     */
    public OrdenTrabajo {
        if (id <= 0) {
            throw new IllegalArgumentException("El id de la orden debe ser positivo, pero fue " + id);
        }
        if (indiceMaquina < 0) {
            throw new IllegalArgumentException(
                    "El índice de la máquina no puede ser negativo, pero fue " + indiceMaquina);
        }
        if (tickCreacion < 0) {
            throw new IllegalArgumentException(
                    "El tick de creación no puede ser negativo, pero fue " + tickCreacion);
        }
        if (duracionEstimadaTicks <= 0) {
            throw new IllegalArgumentException(
                    "La duración estimada debe ser positiva, pero fue " + duracionEstimadaTicks);
        }
        Objects.requireNonNull(etiquetaMaquina, "La orden debe indicar la etiqueta de la máquina");
        Objects.requireNonNull(prioridad, "La orden debe tener una prioridad");
        Objects.requireNonNull(descripcion, "La orden debe tener una descripción");
    }

    /**
     * Devuelve una etiqueta corta para los paneles de la interfaz, por ejemplo
     * {@code "OT-7 PRENSA-01"}.
     *
     * <p>Las celdas de la cola con prioridad son pequeñas y no alcanzan para la descripción
     * completa.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return identificador de la orden junto con la etiqueta de la máquina
     */
    public String etiquetaCorta() {
        return "OT-" + id + " " + etiquetaMaquina;
    }
}
