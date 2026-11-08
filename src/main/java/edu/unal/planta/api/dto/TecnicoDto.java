package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Tecnico;

/**
 * Un técnico de mantenimiento, libre u ocupado.
 *
 * @param nombre         nombre del técnico
 * @param libre          si está en la cola de técnicos disponibles
 * @param ordenActual    la orden que atiende; {@code null} si está libre
 * @param ticksRestantes ticks que le faltan para terminar; 0 si está libre
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record TecnicoDto(String nombre, boolean libre, OrdenTrabajoDto ordenActual, int ticksRestantes) {

    /**
     * Copia un técnico del modelo.
     *
     * @param tecnico técnico del modelo
     * @return la copia
     */
    public static TecnicoDto desde(Tecnico tecnico) {
        return new TecnicoDto(tecnico.nombre(), tecnico.estaLibre(),
                tecnico.ordenActual().map(OrdenTrabajoDto::desde).orElse(null),
                tecnico.ticksRestantes());
    }
}
