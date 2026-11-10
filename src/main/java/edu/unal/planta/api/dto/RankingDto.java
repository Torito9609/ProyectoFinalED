package edu.unal.planta.api.dto;

import java.util.List;

/**
 * Un ranking de fallos: el global o el de una compañía, completo o desde un puntaje mínimo.
 *
 * <p>Las máquinas vienen en el orden del árbol rojo-negro ({@code TreeSet}) del modelo: puntaje
 * descendente y, en los empates, por compañía y luego por índice. No se ordena nada al armar esta
 * respuesta.</p>
 *
 * @param alcance         {@code "Global"} o el nombre de la compañía
 * @param umbralOverhaul  puntaje desde el que se recomienda overhaul
 * @param maximoOverhauls overhauls tras los cuales se recomienda reemplazo
 * @param maquinas        las máquinas, de la peor a la mejor
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record RankingDto(
        String alcance,
        int umbralOverhaul,
        int maximoOverhauls,
        List<EntradaRankingDto> maquinas) {
}
