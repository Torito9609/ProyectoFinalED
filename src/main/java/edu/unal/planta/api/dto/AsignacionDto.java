package edu.unal.planta.api.dto;

/**
 * Cuerpo de {@code POST /api/companias/{id}/asignar-siguiente}: la cola de técnicos que elige el
 * jugador. No lleva orden ni técnico, porque los entregan las estructuras.
 *
 * @param especialidad {@code MECANICO} o {@code ELECTRICO}, sin distinguir mayúsculas
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record AsignacionDto(String especialidad) {
}
