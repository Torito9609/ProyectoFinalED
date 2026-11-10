package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.OrdenTrabajo;

/**
 * Una orden de trabajo: un evento ya clasificado, o un preventivo.
 *
 * @param id                    identificador de la orden
 * @param indiceMaquina         posición de la máquina en el inventario
 * @param etiquetaMaquina       etiqueta de la máquina
 * @param prioridad             nombre de la prioridad, de {@code P1} a {@code P4}
 * @param descripcion           qué hay que hacer
 * @param tickCreacion          tick en que se creó
 * @param duracionEstimadaTicks cuántos ticks tarda un técnico de la especialidad correcta
 * @param tipoFallo             {@code MECANICO} o {@code ELECTRICO}: la especialidad que pide
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record OrdenTrabajoDto(
        int id,
        int indiceMaquina,
        String etiquetaMaquina,
        String prioridad,
        String descripcion,
        int tickCreacion,
        int duracionEstimadaTicks,
        String tipoFallo) {

    /**
     * Copia una orden del modelo.
     *
     * @param orden orden del modelo
     * @return la copia
     */
    public static OrdenTrabajoDto desde(OrdenTrabajo orden) {
        return new OrdenTrabajoDto(orden.id(), orden.indiceMaquina(), orden.etiquetaMaquina(),
                orden.prioridad().name(), orden.descripcion(), orden.tickCreacion(),
                orden.duracionEstimadaTicks(), orden.tipoFallo().name());
    }
}
