package edu.unal.planta.modelo;

/**
 * Catálogo de los tipos de máquina que se pueden crear.
 *
 * <p>Cada tipo define sus sensores, su intervalo preventivo, su tasa de fallo y su carga inicial.
 * Esos parámetros viven en {@link ConfiguracionPlanta#crearMaquina}, para que todos los valores de
 * la simulación sigan en un solo lugar. El orden de las constantes es el orden del inventario
 * inicial de cada compañía.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum TipoMaquina {

    /** Prensa hidráulica. Criticidad alta. */
    PRENSA("Prensa hidráulica", "PRENSA"),

    /** Torno CNC. Criticidad media. */
    TORNO("Torno CNC", "TORNO"),

    /** Brazo robótico de soldadura. Criticidad media. */
    ROBOT("Brazo robótico de soldadura", "ROBOT"),

    /** Compresor de tornillo. Criticidad alta. */
    COMPRESOR("Compresor de tornillo", "COMPRES"),

    /** Horno de tratamiento térmico. Criticidad media. */
    HORNO("Horno de tratamiento térmico", "HORNO"),

    /** Bomba centrífuga. Criticidad alta. */
    BOMBA("Bomba centrífuga", "BOMBA");

    private final String descripcion;
    private final String prefijo;

    TipoMaquina(String descripcion, String prefijo) {
        this.descripcion = descripcion;
        this.prefijo = prefijo;
    }

    /**
     * Devuelve el nombre legible del tipo.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return descripción, por ejemplo {@code "Prensa hidráulica"}
     */
    public String descripcion() {
        return descripcion;
    }

    /**
     * Devuelve el prefijo de las etiquetas de este tipo.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return prefijo, por ejemplo {@code "PRENSA"} para la etiqueta {@code "PRENSA-01"}
     */
    public String prefijo() {
        return prefijo;
    }
}
