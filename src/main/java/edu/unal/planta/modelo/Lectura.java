package edu.unal.planta.modelo;

import java.util.List;
import java.util.Objects;

/**
 * Medición de todos los sensores de una máquina en un tick.
 *
 * <p>Es el elemento que se guarda en la {@link edu.unal.planta.estructuras.ColaCircular} de cada
 * máquina, así que es la clase que más veces se crea en toda la simulación: una por máquina y por
 * tick.</p>
 *
 * <p>Los valores van en una lista <strong>en el mismo orden que la lista de sensores de la
 * máquina</strong>, de modo que el índice sirve para las dos. No se usa un mapa por nombre de
 * sensor porque la posición ya identifica la variable y recorrer por índice es más directo.</p>
 *
 * @param tick    tick de la simulación en que se tomó la medición
 * @param valores un valor por sensor, en el orden de los sensores de la máquina
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record Lectura(int tick, List<Double> valores) {

    /**
     * Valida la lectura y copia la lista para que no se pueda modificar después.
     *
     * @throws IllegalArgumentException si el tick es negativo o no hay ningún valor
     * @throws NullPointerException     si la lista de valores es nula
     */
    public Lectura {
        Objects.requireNonNull(valores, "La lectura debe traer valores");
        if (tick < 0) {
            throw new IllegalArgumentException("El tick no puede ser negativo: " + tick);
        }
        if (valores.isEmpty()) {
            throw new IllegalArgumentException("La lectura debe traer al menos un valor");
        }
        valores = List.copyOf(valores);
    }

    /**
     * Devuelve el valor medido por un sensor.
     *
     * @param indiceSensor posición del sensor en la lista de sensores de la máquina
     * @return valor medido
     * @throws IndexOutOfBoundsException si el índice no corresponde a ningún sensor
     */
    public double valor(int indiceSensor) {
        return valores.get(indiceSensor);
    }
}
