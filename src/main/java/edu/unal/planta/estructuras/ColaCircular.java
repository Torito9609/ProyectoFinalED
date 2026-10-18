package edu.unal.planta.estructuras;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Cola circular de capacidad fija que <strong>sobrescribe el elemento más antiguo</strong> cuando
 * se llena.
 *
 * <p>Es la única estructura implementada por el equipo, porque la librería estándar de Java no
 * ofrece ninguna con este comportamiento: las colas de {@code java.util} crecen cuando se llenan, y
 * {@code ArrayBlockingQueue} rechaza el elemento nuevo en vez de descartar el más viejo. Aquí se
 * necesita lo contrario, que es como funciona un registrador de datos industrial: la memoria es
 * fija y la lectura nueva desplaza a la más antigua.</p>
 *
 * <h2>Cómo funciona</h2>
 * <p>Internamente hay un arreglo de capacidad fija que nunca se redimensiona. En lugar de mover los
 * elementos, se mueven dos índices:</p>
 * <ul>
 *   <li>{@code frente}: posición del elemento más antiguo, que es el próximo en salir.</li>
 *   <li>{@code final}: posición donde se escribirá el próximo elemento. No se almacena, se calcula
 *       como {@code (frente + tamaño) % capacidad}.</li>
 * </ul>
 *
 * <p>El operador módulo es lo que hace que el arreglo se comporte como un círculo: al llegar a la
 * última posición, el siguiente índice vuelve a cero. Cuando la cola está llena,
 * {@code (frente + capacidad) % capacidad} es igual a {@code frente}, de modo que la posición del
 * final coincide con la del frente: escribir ahí es exactamente sobrescribir el elemento más
 * antiguo.</p>
 *
 * <h2>Complejidad</h2>
 * <p>Encolar, desencolar y consultar son <strong>O(1)</strong> en todos los casos, incluso al
 * sobrescribir. No hay copias ni desplazamientos de elementos. Las únicas operaciones O(n) son las
 * que por naturaleza recorren la cola: {@link #elementosEnOrden()} y {@link #vaciar()}.</p>
 *
 * <p>Esta clase no es segura para uso concurrente. No hace falta: toda la simulación se ejecuta en
 * el hilo de eventos de Swing.</p>
 *
 * @param <T> tipo de los elementos almacenados; en el proyecto son lecturas de sensores
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class ColaCircular<T> {

    /**
     * Arreglo de capacidad fija. Se declara {@code Object[]} y no {@code T[]} porque en Java no se
     * pueden crear arreglos de un tipo genérico; las lecturas se convierten en
     * {@link #elementoEn(int)}, que es la única conversión sin verificar de la clase.
     */
    private final Object[] celdas;

    /** Posición del elemento más antiguo, el próximo en salir. */
    private int frente;

    /** Cantidad de elementos almacenados; nunca supera la capacidad. */
    private int tamano;

    /** Cuántos elementos se han descartado por sobrescritura. Solo informativo, para la vista. */
    private long sobrescrituras;

    /**
     * Crea una cola circular vacía con la capacidad indicada.
     *
     * <p>Complejidad: O(capacidad), por la creación del arreglo interno. Es la única vez que se
     * reserva memoria: la cola nunca crece.</p>
     *
     * @param capacidad número máximo de elementos simultáneos; debe ser al menos 1
     * @throws IllegalArgumentException si la capacidad es menor que 1
     */
    public ColaCircular(int capacidad) {
        if (capacidad < 1) {
            throw new IllegalArgumentException(
                    "La capacidad de la cola circular debe ser al menos 1, pero fue " + capacidad);
        }
        this.celdas = new Object[capacidad];
        this.frente = 0;
        this.tamano = 0;
        this.sobrescrituras = 0;
    }

    /**
     * Agrega un elemento al final de la cola.
     *
     * <p>Si hay espacio, lo escribe en la posición del final y aumenta el tamaño. Si la cola está
     * llena, <strong>sobrescribe el elemento más antiguo</strong>: escribe en la posición del
     * frente, avanza el frente una posición y aumenta el contador de sobrescrituras. El tamaño no
     * cambia, porque entró un elemento y salió otro.</p>
     *
     * <p>Complejidad: O(1) en ambos casos.</p>
     *
     * @param elemento elemento a encolar; no puede ser nulo
     * @throws NullPointerException si el elemento es nulo
     */
    public void encolar(T elemento) {
        Objects.requireNonNull(elemento, "La cola circular no acepta elementos nulos");
        if (estaLlena()) {
            // Con la cola llena, la posición del final coincide con la del frente.
            celdas[frente] = elemento;
            frente = siguiente(frente);
            sobrescrituras++;
        } else {
            celdas[indiceFinal()] = elemento;
            tamano++;
        }
    }

    /**
     * Saca y devuelve el elemento más antiguo.
     *
     * <p>Complejidad: O(1). No se desplazan los demás elementos, solo avanza el índice del
     * frente.</p>
     *
     * @return el elemento que estaba en el frente
     * @throws NoSuchElementException si la cola está vacía
     */
    public T desencolar() {
        if (estaVacia()) {
            throw new NoSuchElementException("No se puede desencolar: la cola circular está vacía");
        }
        T elemento = elementoEn(frente);
        celdas[frente] = null; // libera la referencia para que el recolector de basura pueda actuar
        frente = siguiente(frente);
        tamano--;
        return elemento;
    }

    /**
     * Devuelve el elemento más antiguo sin sacarlo de la cola.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return el elemento que está en el frente
     * @throws NoSuchElementException si la cola está vacía
     */
    public T verFrente() {
        if (estaVacia()) {
            throw new NoSuchElementException("No hay frente: la cola circular está vacía");
        }
        return elementoEn(frente);
    }

    /**
     * Devuelve el elemento más reciente sin sacarlo de la cola.
     *
     * <p>Lo necesita la detección de anomalías, que debe comparar contra los umbrales
     * <em>la última lectura</em> de la máquina, no la más antigua.</p>
     *
     * <p>Complejidad: O(1), porque la posición se calcula con aritmética de índices.</p>
     *
     * @return el último elemento encolado
     * @throws NoSuchElementException si la cola está vacía
     */
    public T verMasReciente() {
        if (estaVacia()) {
            throw new NoSuchElementException("No hay elemento reciente: la cola circular está vacía");
        }
        return elementoEn((frente + tamano - 1) % celdas.length);
    }

    /**
     * Indica si la cola no tiene elementos.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return {@code true} si el tamaño es cero
     */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /**
     * Indica si la cola alcanzó su capacidad, es decir si el próximo encolado sobrescribirá.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return {@code true} si el tamaño es igual a la capacidad
     */
    public boolean estaLlena() {
        return tamano == celdas.length;
    }

    /**
     * Devuelve la cantidad de elementos almacenados.
     *
     * <p>Complejidad: O(1), porque el tamaño se mantiene como campo y no se recorre nada.</p>
     *
     * @return número de elementos, entre 0 y la capacidad
     */
    public int tamano() {
        return tamano;
    }

    /**
     * Devuelve la capacidad fijada al construir la cola.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return número máximo de elementos simultáneos
     */
    public int capacidad() {
        return celdas.length;
    }

    /**
     * Devuelve la posición del elemento más antiguo.
     *
     * <p>Lo usa la vista para dibujar el puntero del frente sobre la celda correspondiente. Cuando
     * la cola está vacía el valor no señala ningún elemento, sino la celda donde quedó el frente
     * tras el último desencolado.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return índice del frente dentro del arreglo interno
     */
    public int indiceFrente() {
        return frente;
    }

    /**
     * Devuelve la posición donde se escribirá el próximo elemento.
     *
     * <p>Se calcula como {@code (frente + tamaño) % capacidad}. Cuando la cola está llena coincide
     * con el índice del frente, que es justamente la celda que se va a sobrescribir. La vista dibuja
     * este puntero, y es lo que permite ver el final "dar la vuelta".</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return índice del final dentro del arreglo interno
     */
    public int indiceFinal() {
        return (frente + tamano) % celdas.length;
    }

    /**
     * Devuelve cuántos elementos se han descartado por sobrescritura desde que se creó la cola.
     *
     * <p>Es información para la vista: muestra cuántas lecturas viejas se han perdido, que es el
     * precio de trabajar con memoria fija.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return número de sobrescrituras acumuladas
     */
    public long sobrescrituras() {
        return sobrescrituras;
    }

    /**
     * Devuelve el contenido de una celda física del arreglo interno, que puede estar vacía.
     *
     * <p>Existe para que la vista dibuje el arreglo tal como está en memoria, celda por celda, con
     * sus huecos incluidos. Es la única operación que expone posiciones físicas; el resto de la
     * clase trabaja en orden lógico.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @param indice posición física, entre 0 y {@code capacidad() - 1}
     * @return el elemento de esa celda, o un {@link Optional} vacío si la celda no tiene nada
     * @throws IndexOutOfBoundsException si el índice está fuera del arreglo
     */
    public Optional<T> contenidoCelda(int indice) {
        if (indice < 0 || indice >= celdas.length) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera del arreglo de capacidad " + celdas.length);
        }
        return Optional.ofNullable(elementoEn(indice));
    }

    /**
     * Devuelve los elementos en orden lógico, del más antiguo al más reciente.
     *
     * <p>La lista resultante es independiente de la cola y <strong>no se puede modificar</strong>:
     * la vista recibe una copia de solo lectura, nunca la estructura original.</p>
     *
     * <p>Complejidad: O(n), con n el número de elementos almacenados. Es inevitable, porque hay que
     * recorrerlos todos para copiarlos.</p>
     *
     * @return lista no modificable con los elementos en orden de antigüedad
     */
    public List<T> elementosEnOrden() {
        List<T> copia = new ArrayList<>(tamano);
        for (int i = 0; i < tamano; i++) {
            copia.add(elementoEn((frente + i) % celdas.length));
        }
        return Collections.unmodifiableList(copia);
    }

    /**
     * Deja la cola como recién creada: sin elementos y con el contador de sobrescrituras en cero.
     *
     * <p>Lo usa la acción "Reiniciar" de la simulación.</p>
     *
     * <p>Complejidad: O(capacidad), por limpiar el arreglo interno.</p>
     */
    public void vaciar() {
        Arrays.fill(celdas, null);
        frente = 0;
        tamano = 0;
        sobrescrituras = 0;
    }

    /**
     * Devuelve una descripción del estado interno, útil para depurar y para la bitácora.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return cadena con tamaño, capacidad, punteros y sobrescrituras
     */
    @Override
    public String toString() {
        return "ColaCircular[" + tamano + "/" + celdas.length
                + ", frente=" + frente
                + ", final=" + indiceFinal()
                + ", sobrescrituras=" + sobrescrituras + "]";
    }

    /**
     * Devuelve el elemento de una celda, convertido al tipo de la cola.
     *
     * <p>La conversión es segura porque el arreglo solo se escribe desde {@link #encolar(Object)},
     * que recibe un {@code T}. Es el precio de no poder crear arreglos genéricos en Java.</p>
     *
     * @param indice posición física
     * @return el elemento, o {@code null} si la celda está vacía
     */
    @SuppressWarnings("unchecked")
    private T elementoEn(int indice) {
        return (T) celdas[indice];
    }

    /**
     * Avanza un índice una posición, volviendo a cero al pasar el final del arreglo.
     *
     * <p>Este módulo es lo que convierte el arreglo lineal en un círculo.</p>
     *
     * @param indice posición actual
     * @return la posición siguiente
     */
    private int siguiente(int indice) {
        return (indice + 1) % celdas.length;
    }
}
