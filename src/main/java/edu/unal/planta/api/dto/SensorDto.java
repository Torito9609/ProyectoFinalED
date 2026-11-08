package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Sensor;

/**
 * Definición de un sensor: lo que el frontend necesita para mostrar el valor y colorearlo por zona.
 *
 * @param nombre            nombre de la variable medida
 * @param unidad            unidad de medida
 * @param nominal           valor en operación normal
 * @param umbralAdvertencia desde aquí la zona es de advertencia (P3)
 * @param umbralAlto        desde aquí la zona es alta (P2)
 * @param umbralCritico     desde aquí la zona es crítica (P1)
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record SensorDto(
        String nombre,
        String unidad,
        double nominal,
        double umbralAdvertencia,
        double umbralAlto,
        double umbralCritico) {

    /**
     * Copia la definición de un sensor del modelo.
     *
     * @param sensor sensor del modelo
     * @return la copia
     */
    public static SensorDto desde(Sensor sensor) {
        return new SensorDto(sensor.nombre(), sensor.unidad(), sensor.nominal(),
                sensor.umbralAdvertencia(), sensor.umbralAlto(), sensor.umbralCritico());
    }
}
