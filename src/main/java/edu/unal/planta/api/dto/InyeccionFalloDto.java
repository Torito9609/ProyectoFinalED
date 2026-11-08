package edu.unal.planta.api.dto;

/**
 * Cuerpo de la petición para inyectar un fallo.
 *
 * @param indiceSensor posición del sensor en la máquina
 * @param magnitud     {@code LEVE}, {@code MODERADA} o {@code SEVERA}
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record InyeccionFalloDto(int indiceSensor, String magnitud) {
}
