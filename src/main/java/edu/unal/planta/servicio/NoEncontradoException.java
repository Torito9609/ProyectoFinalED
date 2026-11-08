package edu.unal.planta.servicio;

/**
 * La petición se refiere a algo que no existe, por ejemplo una máquina o un sensor con un índice
 * fuera de rango. La API la convierte en un {@code 404}.
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public class NoEncontradoException extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param mensaje qué no se encontró, en español; llega tal cual al frontend
     */
    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
