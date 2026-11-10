package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Maquina;

/**
 * Una máquina en un ranking de fallos, con su recomendación.
 *
 * <p>La máquina se identifica con su clave compuesta: {@code (idCompania, indice)}.</p>
 *
 * @param posicion        lugar en el ranking, desde 1
 * @param idCompania      id de su compañía
 * @param nombreCompania  nombre de su compañía
 * @param indice          su índice en el inventario de la compañía
 * @param etiqueta        etiqueta de la máquina
 * @param tipo            código del tipo en el catálogo
 * @param estado          nombre del estado, por ejemplo {@code OPERANDO}
 * @param puntajeFallos   puntaje de fallos, la clave del ranking
 * @param numeroOverhauls cuántos overhauls ha recibido
 * @param recomendacion   {@code NINGUNA}, {@code OVERHAUL} o {@code REEMPLAZO}
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EntradaRankingDto(
        int posicion,
        int idCompania,
        String nombreCompania,
        int indice,
        String etiqueta,
        String tipo,
        String estado,
        int puntajeFallos,
        int numeroOverhauls,
        String recomendacion) {

    /**
     * Copia una máquina del ranking.
     *
     * @param posicion       lugar en el ranking, desde 1
     * @param maquina        máquina del modelo
     * @param nombreCompania nombre de su compañía
     * @return la copia
     */
    public static EntradaRankingDto desde(int posicion, Maquina maquina, String nombreCompania) {
        return new EntradaRankingDto(posicion, maquina.idCompania(), nombreCompania, maquina.indice(),
                maquina.etiqueta(), maquina.tipo().name(), maquina.estado().name(),
                maquina.puntajeFallos(), maquina.numeroOverhauls(), maquina.recomendacion().name());
    }
}
