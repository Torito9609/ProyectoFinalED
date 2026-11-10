package edu.unal.planta.estructuras;

/**
 * Árbol de segmentos de tamaño fijo que responde la <strong>suma</strong> y el
 * <strong>máximo</strong> de cualquier rango de posiciones.
 *
 * <p>En el proyecto, cada posición es un tick y cada valor es el puntaje de fallos de ese tick.
 * Así se responde "¿cuántos fallos hubo entre el tick a y el b, y cuál fue el pico?" sin
 * recorrer todos los ticks del rango.</p>
 *
 * <p><strong>Representación.</strong> El árbol vive en dos arreglos paralelos de tamaño
 * {@code 2·m}, donde {@code m} es la menor potencia de 2 que sea mayor o igual que el tamaño
 * pedido. El nodo 1 es la raíz, el padre del nodo {@code i} es {@code i/2} y sus hijos son
 * {@code 2i} y {@code 2i+1}. Las hojas ocupan las celdas {@code [m, 2m)}: la posición {@code p}
 * está en la hoja {@code m + p}. Cada nodo interno guarda la suma y el máximo de las hojas que
 * cubre. No hay objetos nodo ni referencias: igual que en un montículo, la forma del árbol
 * sale de la aritmética de índices. La celda 0 no se usa.</p>
 *
 * <p>Las hojas de relleno (de {@code tamano} a {@code m}) quedan en 0 y ninguna consulta las
 * incluye, porque los rangos válidos terminan antes de {@code tamano}.</p>
 *
 * <p><strong>Por qué el máximo justifica la estructura.</strong> Si solo se pidieran sumas,
 * bastaría un arreglo de sumas prefijas. El máximo de un rango arbitrario no se puede obtener
 * con sumas prefijas; el árbol lo responde en O(log n).</p>
 *
 * <p>Complejidades: construir O(n); leer una posición O(1); actualizar una posición, sumar un
 * rango y obtener el máximo de un rango, O(log n).</p>
 */
public final class ArbolSegmentos {

    /** Número de posiciones útiles (el horizonte de ticks). */
    private final int tamano;

    /** Número de hojas: la menor potencia de 2 mayor o igual que {@link #tamano}. */
    private final int hojas;

    /** Suma de las hojas que cubre cada nodo. Es {@code long} porque la suma de muchos ticks puede crecer. */
    private final long[] suma;

    /** Máximo de las hojas que cubre cada nodo. */
    private final int[] maximo;

    /**
     * Crea un árbol con {@code tamano} posiciones, todas en 0.
     *
     * <p>Complejidad: O(n), por la creación de los arreglos.</p>
     *
     * @param tamano número de posiciones; debe ser al menos 1
     * @throws IllegalArgumentException si el tamaño es menor que 1
     */
    public ArbolSegmentos(int tamano) {
        if (tamano < 1) {
            throw new IllegalArgumentException(
                    "El tamaño del árbol de segmentos debe ser al menos 1, se recibió " + tamano);
        }
        this.tamano = tamano;
        int m = 1;
        while (m < tamano) {
            m *= 2;
        }
        this.hojas = m;
        this.suma = new long[2 * m];
        this.maximo = new int[2 * m];
    }

    /**
     * Devuelve el número de posiciones útiles.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return el tamaño pedido al construir el árbol
     */
    public int tamano() {
        return tamano;
    }

    /**
     * Devuelve el valor guardado en una posición.
     *
     * <p>Complejidad: O(1): se lee directo en la hoja {@code m + posicion}.</p>
     *
     * @param posicion posición entre 0 y {@code tamano - 1}
     * @return el valor de esa posición
     * @throws IndexOutOfBoundsException si la posición está fuera del árbol
     */
    public int valor(int posicion) {
        validarPosicion(posicion);
        return maximo[hojas + posicion];
    }

    /**
     * Asigna un valor a una posición y recalcula los nodos que la cubren.
     *
     * <p>Escribe la hoja y sube hasta la raíz. En cada padre recalcula
     * {@code suma = izquierdo + derecho} y {@code maximo = max(izquierdo, derecho)}.</p>
     *
     * <p>Complejidad: O(log n): un paso por nivel del árbol.</p>
     *
     * @param posicion posición entre 0 y {@code tamano - 1}
     * @param valor valor nuevo; reemplaza al anterior
     * @throws IndexOutOfBoundsException si la posición está fuera del árbol
     */
    public void actualizar(int posicion, int valor) {
        validarPosicion(posicion);
        int nodo = hojas + posicion;
        suma[nodo] = valor;
        maximo[nodo] = valor;
        nodo /= 2;
        while (nodo >= 1) {
            suma[nodo] = suma[2 * nodo] + suma[2 * nodo + 1];
            maximo[nodo] = Math.max(maximo[2 * nodo], maximo[2 * nodo + 1]);
            nodo /= 2;
        }
    }

    /**
     * Suma los valores del rango cerrado {@code [desde, hasta]}.
     *
     * <p>Complejidad: O(log n). Ver {@link #sumaRecursiva} para los tres casos de la recursión.</p>
     *
     * @param desde primera posición del rango, incluida
     * @param hasta última posición del rango, incluida
     * @return la suma de los valores del rango
     * @throws IllegalArgumentException si {@code desde > hasta}
     * @throws IndexOutOfBoundsException si alguna posición está fuera del árbol
     */
    public long suma(int desde, int hasta) {
        validarRango(desde, hasta);
        return sumaRecursiva(1, 0, hojas - 1, desde, hasta);
    }

    /**
     * Devuelve el mayor valor del rango cerrado {@code [desde, hasta]}.
     *
     * <p>Complejidad: O(log n). Ver {@link #maximoRecursivo} para los tres casos de la recursión.</p>
     *
     * @param desde primera posición del rango, incluida
     * @param hasta última posición del rango, incluida
     * @return el máximo de los valores del rango
     * @throws IllegalArgumentException si {@code desde > hasta}
     * @throws IndexOutOfBoundsException si alguna posición está fuera del árbol
     */
    public int maximo(int desde, int hasta) {
        validarRango(desde, hasta);
        return maximoRecursivo(1, 0, hojas - 1, desde, hasta);
    }

    /**
     * Suma la parte del rango pedido que cae dentro del nodo, que cubre {@code [inicio, fin]}.
     *
     * <p>Hay tres casos:</p>
     * <ol>
     *   <li>el nodo queda fuera del rango: aporta 0, el neutro de la suma;</li>
     *   <li>el nodo queda dentro del rango: devuelve su suma guardada, sin bajar más;</li>
     *   <li>el rango corta el nodo: baja a los dos hijos y suma sus resultados.</li>
     * </ol>
     * <p>En cada nivel, a lo sumo dos nodos caen en el tercer caso, así que se visitan O(log n) nodos.</p>
     */
    private long sumaRecursiva(int nodo, int inicio, int fin, int desde, int hasta) {
        if (fin < desde || hasta < inicio) {
            return 0;
        }
        if (desde <= inicio && fin <= hasta) {
            return suma[nodo];
        }
        int medio = (inicio + fin) / 2;
        return sumaRecursiva(2 * nodo, inicio, medio, desde, hasta)
                + sumaRecursiva(2 * nodo + 1, medio + 1, fin, desde, hasta);
    }

    /**
     * Máximo de la parte del rango pedido que cae dentro del nodo, que cubre {@code [inicio, fin]}.
     *
     * <p>Mismos tres casos que {@link #sumaRecursiva}. El neutro del máximo es
     * {@link Integer#MIN_VALUE}, porque no le gana a ningún valor.</p>
     */
    private int maximoRecursivo(int nodo, int inicio, int fin, int desde, int hasta) {
        if (fin < desde || hasta < inicio) {
            return Integer.MIN_VALUE;
        }
        if (desde <= inicio && fin <= hasta) {
            return maximo[nodo];
        }
        int medio = (inicio + fin) / 2;
        return Math.max(
                maximoRecursivo(2 * nodo, inicio, medio, desde, hasta),
                maximoRecursivo(2 * nodo + 1, medio + 1, fin, desde, hasta));
    }

    /** Verifica que la posición esté en {@code [0, tamano)}. */
    private void validarPosicion(int posicion) {
        if (posicion < 0 || posicion >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Posición " + posicion + " fuera del árbol de segmentos de tamaño " + tamano);
        }
    }

    /** Verifica que el rango sea válido: ambas posiciones dentro del árbol y {@code desde <= hasta}. */
    private void validarRango(int desde, int hasta) {
        validarPosicion(desde);
        validarPosicion(hasta);
        if (desde > hasta) {
            throw new IllegalArgumentException(
                    "Rango inválido: desde (" + desde + ") es mayor que hasta (" + hasta + ")");
        }
    }
}
