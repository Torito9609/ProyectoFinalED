package edu.unal.planta.api.dto;

/**
 * Cuerpo de {@code POST /api/companias}.
 *
 * @param nombre nombre de la compañía nueva; debe ser único en la organización, sin distinguir
 *               mayúsculas
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record CrearCompaniaDto(String nombre) {
}
