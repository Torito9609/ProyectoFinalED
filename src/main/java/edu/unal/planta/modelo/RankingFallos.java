package edu.unal.planta.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Ranking de las máquinas de una compañía por puntaje de fallos, de la peor a la mejor.
 *
 * <p><strong>Estructura:</strong> un {@link TreeSet}, que internamente es un árbol rojo-negro, es
 * decir un árbol binario de búsqueda balanceado. El orden siempre está listo: no hay que ordenar
 * nada cuando el frontend pide el ranking, solo recorrer el árbol en orden. Su altura queda acotada
 * a {@code 2·log2(n+1)} aunque las máquinas lleguen ordenadas, así que insertar y eliminar cuestan
 * O(log n).</p>
 *
 * <p><strong>Por qué no ordenar el inventario en cada consulta:</strong> costaría O(n log n) por
 * consulta, y el frontend consulta en cada sondeo. Los fallos son mucho menos frecuentes que las
 * consultas, así que conviene pagar O(log n) por fallo y tener el orden listo.</p>
 *
 * <p><strong>Orden:</strong> puntaje descendente y, en empate, etiqueta ascendente. El desempate es
 * obligatorio: el {@code TreeSet} considera iguales dos elementos que el comparador declara
 * iguales, así que sin él dos máquinas con el mismo puntaje se tratarían como una sola y la segunda
 * no entraría al árbol. Dentro de una compañía las etiquetas son únicas.</p>
 *
 * <p><strong>La trampa de la clave que cambia.</strong> El puntaje es la clave de orden. Si
 * cambiara con la máquina dentro del árbol, el árbol quedaría desordenado y {@code remove} dejaría
 * de encontrarla, sin lanzar ningún error. Por eso el puntaje solo cambia en
 * {@link #sumarFallo(Maquina, int)}, que primero saca la máquina, luego suma y luego la vuelve a
 * insertar. {@link Maquina#sumarPuntaje(int)} tiene visibilidad de paquete para que nadie fuera de
 * {@code modelo} pueda saltarse este paso.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class RankingFallos {

    /** Puntaje de mayor a menor y, en empate, etiqueta en orden alfabético. */
    private static final Comparator<Maquina> ORDEN =
            Comparator.comparingInt(Maquina::puntajeFallos).reversed()
                    .thenComparing(Maquina::etiqueta);

    /** Árbol rojo-negro con las máquinas, siempre en orden de ranking. */
    private final TreeSet<Maquina> arbol;

    /**
     * Crea el ranking con las máquinas dadas.
     *
     * <p>Complejidad: O(n log n): n inserciones de O(log n).</p>
     *
     * @param maquinas máquinas de la compañía
     * @throws NullPointerException si la colección es nula
     */
    public RankingFallos(Collection<Maquina> maquinas) {
        this.arbol = new TreeSet<>(ORDEN);
        this.arbol.addAll(Objects.requireNonNull(maquinas, "Las máquinas no pueden ser nulas"));
    }

    /**
     * Suma un fallo a una máquina y la reubica en el ranking: <strong>eliminar → sumar →
     * insertar</strong>.
     *
     * <p>Es el único lugar donde cambia el puntaje de una máquina.</p>
     *
     * <p>Complejidad: O(log n): una eliminación y una inserción en el árbol.</p>
     *
     * @param maquina máquina que falló; debe estar en el ranking
     * @param peso    puntos del fallo (ver {@link Prioridad#peso()})
     * @throws IllegalStateException si la máquina no está en el ranking
     */
    public void sumarFallo(Maquina maquina, int peso) {
        if (!arbol.remove(maquina)) {
            throw new IllegalStateException(
                    "La máquina " + maquina.etiqueta() + " no está en el ranking: "
                            + "su puntaje cambió por fuera de RankingFallos o nunca se insertó");
        }
        maquina.sumarPuntaje(peso);
        arbol.add(maquina);
    }

    /**
     * Devuelve el ranking completo, de la máquina con más fallos a la de menos.
     *
     * <p>Complejidad: O(n): recorrido en orden del árbol.</p>
     *
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> enOrden() {
        return Collections.unmodifiableList(new ArrayList<>(arbol));
    }

    /**
     * Devuelve las máquinas cuyo puntaje es mayor o igual que el umbral: las candidatas a overhaul.
     *
     * <p>Como el orden es descendente, esas máquinas están al principio del árbol. El recorrido
     * empieza en la primera y se detiene en la primera que queda por debajo del umbral, sin visitar
     * las demás. Es lo mismo que daría {@code headSet}, sin tener que fabricar una máquina falsa
     * que sirva de frontera.</p>
     *
     * <p>Complejidad: O(log n + k), con k el número de máquinas devueltas.</p>
     *
     * @param umbral puntaje mínimo
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> conPuntajeMinimo(int umbral) {
        List<Maquina> candidatas = new ArrayList<>();
        for (Maquina maquina : arbol) {
            if (maquina.puntajeFallos() < umbral) {
                break;
            }
            candidatas.add(maquina);
        }
        return Collections.unmodifiableList(candidatas);
    }

    /**
     * Vacía el ranking y vuelve a insertar las máquinas, con su puntaje actual.
     *
     * <p>Sirve cuando el puntaje de muchas máquinas cambió de una vez, como al reiniciar la
     * simulación. En ese caso no se puede eliminar cada máquina con {@code remove}, porque su clave
     * ya cambió y el árbol no la encontraría; hay que descartar el árbol entero y armarlo de
     * nuevo.</p>
     *
     * <p>Complejidad: O(n log n).</p>
     *
     * @param maquinas máquinas de la compañía
     */
    public void reconstruir(Collection<Maquina> maquinas) {
        arbol.clear();
        arbol.addAll(maquinas);
    }

    /**
     * Devuelve cuántas máquinas hay en el ranking.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return número de máquinas
     */
    public int tamano() {
        return arbol.size();
    }
}
