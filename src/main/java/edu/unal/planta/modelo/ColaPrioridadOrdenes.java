package edu.unal.planta.modelo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Cola con prioridad de órdenes de trabajo, construida como un <strong>arreglo de cuatro colas
 * FIFO</strong>: una por nivel de prioridad, con el índice igual a {@link Prioridad#nivel()}.
 *
 * <h2>Por qué así y no con {@code PriorityQueue}</h2>
 * <p>La librería estándar ofrece {@code PriorityQueue}, pero se descartó por dos razones:</p>
 * <ol>
 *   <li>Internamente es un <em>heap</em>, una estructura no lineal que el curso aún no ha visto y que
 *       queda fuera del alcance de esta entrega.</li>
 *   <li>Más importante: <strong>no garantiza el orden de llegada</strong> entre elementos de igual
 *       prioridad. Dos alarmas P2 podrían atenderse en orden invertido, lo que en una planta real es
 *       inaceptable.</li>
 * </ol>
 *
 * <p>Con un arreglo de colas FIFO ese problema desaparece por construcción: el orden de llegada
 * dentro de cada prioridad se cumple solo, porque cada nivel <em>es</em> una cola FIFO. No hay que
 * programar ningún criterio de desempate.</p>
 *
 * <p>También se descartó mantener un arreglo ordenado por prioridad, porque insertar costaría O(n)
 * por el desplazamiento de los elementos.</p>
 *
 * <h2>Complejidad</h2>
 * <ul>
 *   <li><strong>Insertar: O(1).</strong> El nivel da el índice del arreglo y encolar en un
 *       {@code ArrayDeque} es O(1).</li>
 *   <li><strong>Extraer: O(k), con k = 4.</strong> En el peor caso revisa los cuatro niveles antes
 *       de encontrar el primero no vacío. Como k es una constante fijada por el enum y no crece con
 *       la cantidad de órdenes, en la práctica es tiempo constante.</li>
 * </ul>
 *
 * <p>Cada nivel usa {@code ArrayDeque} y no {@code LinkedList} porque la lista enlazada crea un
 * objeto nodo por elemento, mientras que {@code ArrayDeque} guarda los elementos en un arreglo
 * interno.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class ColaPrioridadOrdenes implements ColaPrioridadSoloLectura {

    /**
     * Una cola FIFO por nivel de prioridad. La posición del arreglo <strong>es</strong> el nivel:
     * {@code niveles[0]} guarda las P1, {@code niveles[3]} las P4.
     */
    private final Deque<OrdenTrabajo>[] niveles;

    /**
     * Crea la cola con los cuatro niveles vacíos.
     *
     * <p>Complejidad: O(k), con k = 4, por crear una cola por nivel.</p>
     */
    @SuppressWarnings("unchecked")
    public ColaPrioridadOrdenes() {
        // Java no permite crear arreglos de un tipo genérico; se crea el arreglo crudo y se
        // convierte. Es seguro porque el arreglo es privado y solo guarda colas de órdenes.
        niveles = new ArrayDeque[Prioridad.NIVELES];
        for (int i = 0; i < niveles.length; i++) {
            niveles[i] = new ArrayDeque<>();
        }
    }

    /**
     * Inserta una orden al final de la cola de su nivel de prioridad.
     *
     * <p>Complejidad: O(1). El nivel de la orden se usa como índice directo del arreglo, así que no
     * hay búsqueda ni comparaciones con las órdenes ya almacenadas.</p>
     *
     * @param orden orden a insertar; no puede ser nula
     * @throws NullPointerException si la orden es nula
     */
    public void insertar(OrdenTrabajo orden) {
        Objects.requireNonNull(orden, "No se puede insertar una orden nula");
        niveles[orden.prioridad().nivel()].addLast(orden);
    }

    /**
     * Saca la orden más prioritaria y, dentro de su nivel, la que llegó primero.
     *
     * <p>Recorre los niveles empezando por P1 y devuelve el frente de la primera cola no vacía.</p>
     *
     * <p>Complejidad: O(k), con k = 4.</p>
     *
     * @return la orden extraída, o un {@link Optional} vacío si no hay órdenes pendientes
     */
    public Optional<OrdenTrabajo> extraer() {
        for (Deque<OrdenTrabajo> cola : niveles) {
            if (!cola.isEmpty()) {
                return Optional.of(cola.removeFirst());
            }
        }
        return Optional.empty();
    }

    /**
     * Devuelve la próxima orden en salir, sin extraerla.
     *
     * <p>Complejidad: O(k), con k = 4.</p>
     *
     * @return la siguiente orden, o un {@link Optional} vacío si no hay órdenes pendientes
     */
    @Override
    public Optional<OrdenTrabajo> verSiguiente() {
        for (Deque<OrdenTrabajo> cola : niveles) {
            if (!cola.isEmpty()) {
                return Optional.of(cola.peekFirst());
            }
        }
        return Optional.empty();
    }

    /**
     * Devuelve el nivel del que saldrá la próxima orden.
     *
     * <p>Lo usa la vista para resaltar el carril activo entre los cuatro que dibuja.</p>
     *
     * <p>Complejidad: O(k), con k = 4.</p>
     *
     * @return la prioridad del próximo en salir, o un {@link Optional} vacío si la cola está vacía
     */
    @Override
    public Optional<Prioridad> nivelDelSiguiente() {
        return verSiguiente().map(OrdenTrabajo::prioridad);
    }

    /**
     * Devuelve el total de órdenes pendientes en los cuatro niveles.
     *
     * <p>Complejidad: O(k), con k = 4. Se suman los tamaños en vez de mantener un contador aparte,
     * para no tener que sostener un dato que podría quedar desincronizado.</p>
     *
     * @return cantidad total de órdenes pendientes
     */
    @Override
    public int tamano() {
        int total = 0;
        for (Deque<OrdenTrabajo> cola : niveles) {
            total += cola.size();
        }
        return total;
    }

    /**
     * Devuelve cuántas órdenes hay pendientes en un nivel.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @param prioridad nivel consultado
     * @return cantidad de órdenes en ese nivel
     * @throws NullPointerException si la prioridad es nula
     */
    @Override
    public int tamanoNivel(Prioridad prioridad) {
        Objects.requireNonNull(prioridad, "La prioridad no puede ser nula");
        return niveles[prioridad.nivel()].size();
    }

    /**
     * Indica si no hay ninguna orden pendiente.
     *
     * <p>Complejidad: O(k), con k = 4.</p>
     *
     * @return {@code true} si los cuatro niveles están vacíos
     */
    @Override
    public boolean estaVacia() {
        for (Deque<OrdenTrabajo> cola : niveles) {
            if (!cola.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Devuelve las órdenes de un nivel, de la primera a la última en salir.
     *
     * <p>La lista es una copia <strong>no modificable</strong>: la vista dibuja el carril sin poder
     * alterar la cola, tal como exige la arquitectura del proyecto.</p>
     *
     * <p>Complejidad: O(n), con n el número de órdenes de ese nivel.</p>
     *
     * @param prioridad nivel consultado
     * @return lista no modificable con las órdenes del nivel, en orden FIFO
     * @throws NullPointerException si la prioridad es nula
     */
    @Override
    public List<OrdenTrabajo> ordenesDe(Prioridad prioridad) {
        Objects.requireNonNull(prioridad, "La prioridad no puede ser nula");
        return List.copyOf(niveles[prioridad.nivel()]);
    }

    /**
     * Elimina todas las órdenes de los cuatro niveles.
     *
     * <p>Lo usa la acción "Reiniciar" de la simulación.</p>
     *
     * <p>Complejidad: O(n), con n el total de órdenes almacenadas.</p>
     */
    public void vaciar() {
        for (Deque<OrdenTrabajo> cola : niveles) {
            cola.clear();
        }
    }

    /**
     * Devuelve el tamaño de cada nivel, útil para depurar y para la bitácora.
     *
     * <p>Complejidad: O(k), con k = 4.</p>
     *
     * @return cadena con la forma {@code ColaPrioridadOrdenes[P1=1, P2=0, P3=2, P4=0]}
     */
    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder("ColaPrioridadOrdenes[");
        for (Prioridad prioridad : Prioridad.values()) {
            if (prioridad.nivel() > 0) {
                texto.append(", ");
            }
            texto.append(prioridad.name()).append('=').append(niveles[prioridad.nivel()].size());
        }
        return texto.append(']').toString();
    }
}
