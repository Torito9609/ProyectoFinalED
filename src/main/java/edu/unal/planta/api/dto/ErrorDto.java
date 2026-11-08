package edu.unal.planta.api.dto;

/**
 * Cuerpo de una respuesta de error.
 *
 * @param estado  código HTTP: 400, 404 o 409
 * @param mensaje qué pasó, en español, para mostrarlo al usuario
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ErrorDto(int estado, String mensaje) {
}
