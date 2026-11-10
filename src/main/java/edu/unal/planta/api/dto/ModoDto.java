package edu.unal.planta.api.dto;

/**
 * Cuerpo de {@code PUT /api/simulacion/modo}.
 *
 * @param modo {@code AUTOMATICO} o {@code MANUAL}, sin distinguir mayúsculas
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ModoDto(String modo) {
}
