package edu.unal.planta.api.dto;

import java.util.List;

/**
 * Un nivel de la cola con prioridad: una fila FIFO de órdenes de la misma prioridad.
 *
 * @param prioridad   nombre de la prioridad, de {@code P1} a {@code P4}
 * @param descripcion descripción de la prioridad, por ejemplo {@code CRÍTICA}
 * @param ordenes     órdenes del carril, de la primera a la última en salir
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record CarrilDto(String prioridad, String descripcion, List<OrdenTrabajoDto> ordenes) {
}
