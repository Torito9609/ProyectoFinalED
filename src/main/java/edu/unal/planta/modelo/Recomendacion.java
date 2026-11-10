package edu.unal.planta.modelo;

/**
 * Qué se le recomienda a una máquina según su historial de fallos.
 *
 * <p>Es un dato <strong>derivado</strong>: lo calcula {@link Maquina#recomendacion()} a partir del
 * puntaje de fallos y del número de overhauls, y no se guarda en ningún lado. La recomendación no
 * asigna a nadie: el superusuario o la compañía decide si la aprueba.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum Recomendacion {

    /** El puntaje de fallos está por debajo del umbral: no hace falta nada. */
    NINGUNA("Ninguna"),

    /** El puntaje superó el umbral: conviene una reparación mayor por técnicos del fabricante. */
    OVERHAUL("Overhaul"),

    /** El puntaje superó el umbral y la máquina ya agotó sus overhauls: conviene reemplazarla. */
    REEMPLAZO("Reemplazo");

    private final String descripcion;

    Recomendacion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el nombre legible de la recomendación.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return descripción para mostrar en la interfaz
     */
    public String descripcion() {
        return descripcion;
    }
}
