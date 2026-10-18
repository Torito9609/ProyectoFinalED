package edu.unal.planta.modelo;

/**
 * Gravedad de una anomalía física en una máquina.
 *
 * <p>La magnitud determina cuánto se desvía la lectura respecto de su valor nominal, y por lo tanto
 * en qué zona de umbrales termina cayendo: una anomalía leve lleva la lectura entre el umbral de
 * advertencia y el alto, una moderada entre el alto y el crítico, y una severa por encima del
 * crítico.</p>
 *
 * <p>Importante para la defensa del proyecto: la magnitud <strong>no</strong> es la prioridad. El
 * clasificador nunca la consulta; solo compara el valor leído contra los umbrales del sensor. Por
 * eso el sistema funcionaría igual con datos de un sensor real, donde nadie sabe de antemano qué
 * tan grave es el fallo.</p>
 *
 * <p>Los pesos con que el generador elige entre las tres magnitudes están en
 * {@link ConfiguracionPlanta}, junto con los demás parámetros de la simulación.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum Magnitud {

    /** Desviación pequeña: la lectura llega a la zona de advertencia. */
    LEVE("Leve"),

    /** Desviación intermedia: la lectura llega a la zona alta. */
    MODERADA("Moderada"),

    /** Desviación grande: la lectura supera el umbral crítico. */
    SEVERA("Severa");

    private final String descripcion;

    Magnitud(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el nombre legible de la magnitud.
     *
     * @return descripción para mostrar en la interfaz
     */
    public String descripcion() {
        return descripcion;
    }
}
