package edu.unal.planta.api.dto;

/**
 * Cuerpo de {@code POST /api/overhauls}: la máquina, por su clave compuesta.
 *
 * <p>Los campos son {@code Integer} y no {@code int} para distinguir un campo que falta (que
 * responde {@code 400}) de la compañía o la máquina 0.</p>
 *
 * @param idCompania    id de la compañía
 * @param indiceMaquina índice de la máquina en el inventario de la compañía
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record AprobarOverhaulDto(Integer idCompania, Integer indiceMaquina) {
}
