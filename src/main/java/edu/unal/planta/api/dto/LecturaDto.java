package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Lectura;
import java.util.List;

/**
 * Una lectura de todos los sensores de una máquina en un tick.
 *
 * @param tick    tick en que se tomó
 * @param valores un valor por sensor, en el mismo orden que los sensores de la máquina
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record LecturaDto(int tick, List<Double> valores) {

    /**
     * Copia una lectura del modelo.
     *
     * @param lectura lectura del modelo
     * @return la copia
     */
    public static LecturaDto desde(Lectura lectura) {
        return new LecturaDto(lectura.tick(), List.copyOf(lectura.valores()));
    }
}
