package edu.unal.planta.modelo;

/**
 * Tipo de un fallo, y con él la especialidad del técnico que lo repara.
 *
 * <p>Cada {@link Sensor} declara su tipo de fallo en el catálogo de {@link ConfiguracionPlanta}:
 * una anomalía en ese sensor es un fallo de ese tipo. El tipo no cambia la prioridad, que sigue
 * dependiendo solo del valor contra los umbrales: decide <strong>quién</strong> repara, no
 * <strong>qué tan urgente</strong> es.</p>
 *
 * <p>El mismo enumerado sirve de especialidad del {@link Tecnico}: un técnico mecánico es el que
 * repara fallos mecánicos. Si atiende un fallo del otro tipo, tarda más
 * ({@link ConfiguracionPlanta#PENALIDAD_ESPECIALIDAD}).</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum TipoFallo {

    /** Desgaste, rodamientos, sistema hidráulico, presión, vibración. */
    MECANICO("mecánico"),

    /** Motores, corriente, instrumentación y lazos de control. */
    ELECTRICO("eléctrico");

    private final String descripcion;

    TipoFallo(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el nombre legible del tipo, en singular.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return descripción, por ejemplo {@code "eléctrico"}
     */
    public String descripcion() {
        return descripcion;
    }
}
