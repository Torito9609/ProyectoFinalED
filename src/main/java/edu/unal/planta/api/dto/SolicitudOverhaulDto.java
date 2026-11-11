package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.SolicitudOverhaul;

/**
 * Una solicitud de overhaul aprobada, que espera en el montículo o que atiende un técnico del
 * fabricante.
 *
 * @param idCompania       id de la compañía de la máquina
 * @param nombreCompania   nombre de la compañía
 * @param indiceMaquina    índice de la máquina en su inventario
 * @param etiqueta         etiqueta de la máquina
 * @param puntajeCongelado puntaje de fallos al aprobar: la clave del montículo
 * @param secuencia        número de llegada, que desempata entre puntajes iguales
 * @param tickAprobacion   tick en que se aprobó
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record SolicitudOverhaulDto(
        int idCompania,
        String nombreCompania,
        int indiceMaquina,
        String etiqueta,
        int puntajeCongelado,
        long secuencia,
        int tickAprobacion) {

    /**
     * Copia una solicitud del modelo.
     *
     * @param solicitud      solicitud del modelo
     * @param nombreCompania nombre de su compañía
     * @return la copia
     */
    public static SolicitudOverhaulDto desde(SolicitudOverhaul solicitud, String nombreCompania) {
        return new SolicitudOverhaulDto(solicitud.idCompania(), nombreCompania, solicitud.indiceMaquina(),
                solicitud.etiqueta(), solicitud.puntajeCongelado(), solicitud.secuencia(),
                solicitud.tickAprobacion());
    }
}
