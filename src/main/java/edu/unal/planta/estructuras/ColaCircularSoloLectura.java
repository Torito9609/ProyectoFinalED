package edu.unal.planta.estructuras;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones de consulta de una cola circular, sin ninguna que modifique su contenido.
 *
 * <p>Existe para cumplir una regla de arquitectura del proyecto: <strong>la vista solo lee el
 * modelo</strong>. Si un panel recibiera la {@link ColaCircular} completa, nada le impediría llamar
 * a {@code encolar} o {@code desencolar} y alterar la simulación desde el lugar equivocado. Al
 * entregarle esta interfaz, el compilador hace cumplir la regla: esos métodos simplemente no
 * existen para quien la recibe.</p>
 *
 * <p>Es un uso directo de dos ideas de programación orientada a objetos:</p>
 * <ul>
 *   <li><strong>Encapsulamiento:</strong> se expone lo que el cliente necesita y se esconde lo
 *       demás, aunque el objeto sea el mismo.</li>
 *   <li><strong>Polimorfismo:</strong> {@link ColaCircular} implementa esta interfaz, así que una
 *       misma cola se comporta como estructura completa para el modelo y como estructura de solo
 *       consulta para la vista, sin duplicar ni copiar nada.</li>
 * </ul>
 *
 * <p>Todas las operaciones de esta interfaz son O(1), salvo {@link #elementosEnOrden()}, que es
 * O(n) porque construye una copia.</p>
 *
 * @param <T> tipo de los elementos almacenados
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public interface ColaCircularSoloLectura<T> {

    /**
     * Devuelve la capacidad fija de la cola.
     *
     * @return número máximo de elementos simultáneos
     */
    int capacidad();

    /**
     * Devuelve la cantidad de elementos almacenados.
     *
     * @return número de elementos, entre 0 y la capacidad
     */
    int tamano();

    /**
     * Indica si la cola no tiene elementos.
     *
     * @return {@code true} si el tamaño es cero
     */
    boolean estaVacia();

    /**
     * Indica si la cola alcanzó su capacidad.
     *
     * @return {@code true} si el próximo encolado sobrescribirá
     */
    boolean estaLlena();

    /**
     * Devuelve la posición del elemento más antiguo.
     *
     * @return índice del frente dentro del arreglo interno
     */
    int indiceFrente();

    /**
     * Devuelve la posición donde se escribirá el próximo elemento.
     *
     * @return índice del final dentro del arreglo interno
     */
    int indiceFinal();

    /**
     * Devuelve cuántos elementos se han descartado por sobrescritura.
     *
     * @return número de sobrescrituras acumuladas
     */
    long sobrescrituras();

    /**
     * Devuelve el elemento más antiguo sin sacarlo.
     *
     * @return el elemento que está en el frente
     * @throws java.util.NoSuchElementException si la cola está vacía
     */
    T verFrente();

    /**
     * Devuelve el elemento más reciente sin sacarlo.
     *
     * @return el último elemento encolado
     * @throws java.util.NoSuchElementException si la cola está vacía
     */
    T verMasReciente();

    /**
     * Devuelve el contenido de una celda física del arreglo interno, que puede estar vacía.
     *
     * @param indice posición física, entre 0 y {@code capacidad() - 1}
     * @return el elemento de esa celda, o un {@link Optional} vacío si la celda no tiene nada
     * @throws IndexOutOfBoundsException si el índice está fuera del arreglo
     */
    Optional<T> contenidoCelda(int indice);

    /**
     * Devuelve los elementos en orden lógico, del más antiguo al más reciente.
     *
     * @return lista no modificable con los elementos en orden de antigüedad
     */
    List<T> elementosEnOrden();
}
