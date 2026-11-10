package edu.unal.planta.api.dto;

/**
 * Estado de la simulación, sin el estado de la planta.
 *
 * <p>Es la respuesta de los controles (iniciar, pausar, paso, reiniciar, cambiar el modo) y de
 * {@code GET /api/simulacion}: lo mínimo para actualizar la barra de controles sin copiar toda la
 * planta.</p>
 *
 * @param tick             tick actual de la partida
 * @param semilla          semilla maestra de la simulación
 * @param enEjecucion      si el reloj está avanzando la simulación
 * @param modo             {@code AUTOMATICO} o {@code MANUAL}
 * @param duracionPartida  ticks que dura una partida
 * @param partidaTerminada si el tick llegó a la duración de la partida; entonces el reloj se pausa
 *                         y solo reiniciar empieza otra
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EstadoSimulacionDto(int tick, long semilla, boolean enEjecucion, String modo,
                                  int duracionPartida, boolean partidaTerminada) {
}
