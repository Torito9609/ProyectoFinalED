package edu.unal.planta.modelo.comandos;

/**
 * Una acción del operador que se puede deshacer.
 *
 * <p>Es el patrón Comando: cada acción se convierte en un objeto que sabe ejecutarse y revertirse.
 * Al ejecutarse, el comando se apila; deshacer es sacar el de la cima de la pila y pedirle que se
 * revierta. La última acción es la primera en revertirse, que es el comportamiento LIFO de una
 * pila.</p>
 *
 * <p>El método {@link #sePuedeDeshacer()} resuelve un caso borde real: entre el momento en que el
 * operador ejecuta una acción y el momento en que la deshace, la simulación sigue corriendo y pudo
 * haber cambiado el estado por su cuenta (por ejemplo, una alarma crítica detuvo la máquina). En
 * ese caso revertir produciría un estado inconsistente, así que el comando se descarta y queda
 * registrado en la bitácora.</p>
 *
 * <p><strong>Qué no es deshacible:</strong> clasificar un evento, asignar una orden o inyectar un
 * fallo. Revertir esas acciones obligaría a reinsertar un elemento por el frente de una cola FIFO,
 * o a sacar de la cola con prioridad un elemento que no es el siguiente. Las dos cosas violan el
 * tipo abstracto de datos, y se prefirió respetarlo antes que ofrecer la funcionalidad.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public interface ComandoOperador {

    /**
     * Aplica la acción sobre el modelo.
     */
    void ejecutar();

    /**
     * Indica si todavía tiene sentido revertir la acción.
     *
     * @return {@code true} si el estado actual sigue siendo el que dejó este comando
     */
    boolean sePuedeDeshacer();

    /**
     * Revierte la acción, devolviendo el modelo al estado anterior.
     *
     * @throws IllegalStateException si ya no se puede deshacer
     */
    void deshacer();

    /**
     * Devuelve una descripción corta de la acción, para mostrarla en la pila de la interfaz.
     *
     * @return texto como {@code "Parar PRENSA-01"}
     */
    String descripcion();
}
