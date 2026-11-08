package edu.unal.planta.servicio;

/**
 * La acción es válida en general, pero no se permite en el estado actual de la simulación, por
 * ejemplo inyectar un fallo en una máquina que ya tiene uno. La API la convierte en un
 * {@code 409}.
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public class AccionNoPermitidaException extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param mensaje por qué no se permite, en español; llega tal cual al frontend
     */
    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
