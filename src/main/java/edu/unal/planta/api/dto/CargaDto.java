package edu.unal.planta.api.dto;

/**
 * Cuerpo de la petición para cambiar la carga de una máquina.
 *
 * @param carga porcentaje nuevo, entre 0 y 100
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record CargaDto(int carga) {
}
