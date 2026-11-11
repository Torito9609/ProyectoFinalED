package edu.unal.planta.api.dto;

/**
 * Un técnico del fabricante, libre u ocupado con un overhaul.
 *
 * @param nombre          nombre del técnico
 * @param libre           si está en la cola FIFO de técnicos del fabricante libres
 * @param solicitudActual el overhaul que atiende; {@code null} si está libre
 * @param ticksRestantes  ticks que le faltan para terminar; 0 si está libre
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record TecnicoExternoDto(String nombre, boolean libre, SolicitudOverhaulDto solicitudActual,
                                int ticksRestantes) {
}
