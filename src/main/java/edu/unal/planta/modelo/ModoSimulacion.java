package edu.unal.planta.modelo;

/**
 * Quién decide el momento de las operaciones de la simulación.
 *
 * <p>En los dos modos las estructuras deciden <strong>qué</strong> elemento sale: la cola de
 * eventos entrega el más antiguo, la cola con prioridad la orden más urgente y la cola de técnicos
 * el que lleva más tiempo esperando. El modo solo cambia <strong>quién</strong> dispara la
 * operación: el reloj o el jugador.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum ModoSimulacion {

    /** El tick genera fallos, clasifica y asigna por su cuenta. */
    AUTOMATICO("automático"),

    /** El jugador inyecta los fallos y decide cuándo clasificar y cuándo asignar. */
    MANUAL("manual");

    private final String descripcion;

    ModoSimulacion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el nombre legible del modo.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return descripción para la bitácora y la interfaz
     */
    public String descripcion() {
        return descripcion;
    }
}
