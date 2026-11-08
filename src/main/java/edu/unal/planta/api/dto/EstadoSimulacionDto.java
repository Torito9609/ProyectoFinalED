package edu.unal.planta.api.dto;

/**
 * Estado de la simulación, sin el estado de la planta.
 *
 * <p>Es la respuesta de los controles (iniciar, pausar, paso, reiniciar) y de
 * {@code GET /api/simulacion}: lo mínimo para actualizar la barra de controles sin copiar toda la
 * planta.</p>
 *
 * @param tick        tick actual
 * @param semilla     semilla de la simulación
 * @param enEjecucion si el reloj está avanzando la simulación
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EstadoSimulacionDto(int tick, long semilla, boolean enEjecucion) {
}
