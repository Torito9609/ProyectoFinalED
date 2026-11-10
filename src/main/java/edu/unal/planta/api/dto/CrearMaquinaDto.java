package edu.unal.planta.api.dto;

/**
 * Cuerpo de {@code POST /api/companias/{id}/maquinas}.
 *
 * @param tipo     código del tipo en el catálogo, por ejemplo {@code TORNO}
 * @param etiqueta etiqueta de la máquina nueva; debe ser única dentro de su compañía y se guarda en
 *                 mayúsculas
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record CrearMaquinaDto(String tipo, String etiqueta) {
}
