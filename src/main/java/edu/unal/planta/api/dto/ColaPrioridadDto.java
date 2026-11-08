package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.ColaPrioridadSoloLectura;
import edu.unal.planta.modelo.Prioridad;
import java.util.ArrayList;
import java.util.List;

/**
 * Foto de la cola con prioridad: los cuatro carriles y cuál es el siguiente en salir.
 *
 * @param carriles  los cuatro carriles, de P1 a P4
 * @param siguiente prioridad del carril del que sale la siguiente orden; {@code null} si no hay
 *                  órdenes
 * @param tamano    total de órdenes pendientes
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ColaPrioridadDto(List<CarrilDto> carriles, String siguiente, int tamano) {

    /**
     * Copia la cola con prioridad, carril por carril.
     *
     * <p>Complejidad: O(n) en el número de órdenes pendientes.</p>
     *
     * @param cola cola con prioridad del modelo, en modo de solo lectura
     * @return la copia
     */
    public static ColaPrioridadDto desde(ColaPrioridadSoloLectura cola) {
        List<CarrilDto> carriles = new ArrayList<>(Prioridad.NIVELES);
        for (Prioridad prioridad : Prioridad.values()) {
            List<OrdenTrabajoDto> ordenes = cola.ordenesDe(prioridad).stream()
                    .map(OrdenTrabajoDto::desde)
                    .toList();
            carriles.add(new CarrilDto(prioridad.name(), prioridad.descripcion(), ordenes));
        }
        String siguiente = cola.nivelDelSiguiente().map(Prioridad::name).orElse(null);
        return new ColaPrioridadDto(List.copyOf(carriles), siguiente, cola.tamano());
    }
}
