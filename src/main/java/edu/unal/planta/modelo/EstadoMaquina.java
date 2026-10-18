package edu.unal.planta.modelo;

/**
 * Estado en que se encuentra una máquina de la planta.
 *
 * <p>Como el enum {@link Prioridad}, guarda su color como entero {@code 0xRRGGBB} y no como un
 * color de Swing, porque el modelo no puede importar {@code java.awt}. El panel de inventario lo
 * convierte para pintar cada celda.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum EstadoMaquina {

    /** Funcionando con normalidad: lee sensores y puede desarrollar anomalías. */
    OPERANDO("Operando", 0x2E7D32),

    /** Tiene una alarma sin atender; sigue leyendo sensores. */
    EN_ALARMA("En alarma", 0xF9A825),

    /** Detenida por una alarma crítica; deja de leer sensores. */
    DETENIDA("Detenida", 0xC62828),

    /** Un técnico está atendiendo su orden de trabajo. */
    EN_MANTENIMIENTO("En mantenimiento", 0x1565C0),

    /** El operador la paró a mano. */
    PARADA_MANUAL("Parada manual", 0x616161);

    private final String descripcion;
    private final int rgb;

    EstadoMaquina(String descripcion, int rgb) {
        this.descripcion = descripcion;
        this.rgb = rgb;
    }

    /**
     * Devuelve el nombre legible del estado.
     *
     * @return descripción para mostrar en la interfaz
     */
    public String descripcion() {
        return descripcion;
    }

    /**
     * Devuelve el color del estado, en formato {@code 0xRRGGBB}.
     *
     * @return color empacado en un entero
     */
    public int rgb() {
        return rgb;
    }

    /**
     * Indica si en este estado la máquina genera lecturas de sensores.
     *
     * <p>Según el paso 2 del tick, solo leen las máquinas operando o en alarma.</p>
     *
     * @return {@code true} si la máquina debe leer sus sensores
     */
    public boolean generaLecturas() {
        return this == OPERANDO || this == EN_ALARMA;
    }
}
