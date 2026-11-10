package edu.unal.planta.api.dto;

import java.util.List;

/**
 * Respuesta de {@code GET /api/partida/resumen}: el resultado de la partida, compañía por compañía.
 *
 * <p>Se puede pedir en cualquier momento; con {@code terminada} en {@code true} es el resultado
 * final. Cada compañía trae su caja, sus ingresos (la producción), sus costos, sus fallos y su
 * disponibilidad.</p>
 *
 * @param tick            tick actual
 * @param duracionPartida ticks que dura una partida
 * @param terminada       si la partida terminó
 * @param modo            {@code AUTOMATICO} o {@code MANUAL}
 * @param companias       los totales de cada compañía, en el orden de sus ids
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ResumenPartidaDto(int tick, int duracionPartida, boolean terminada, String modo,
                                List<ResumenCompaniaDto> companias) {
}
