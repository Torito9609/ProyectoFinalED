package edu.unal.planta.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Ranking de máquinas por puntaje de fallos, de la peor a la mejor.
 *
 * <p>Hay uno por compañía y uno global, que es la misma clase con todas las máquinas de la
 * organización. El de cada compañía conoce al global y lo mantiene al día: cada vez que una
 * máquina entra, sale o cambia de puntaje en su compañía, lo mismo pasa en el global.</p>
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
 * <p><strong>Orden:</strong> puntaje descendente y, en empate, por la clave compuesta de la máquina:
 * {@code idCompania} y luego {@code indice}. El desempate es obligatorio: el {@code TreeSet}
 * considera iguales dos elementos que el comparador declara iguales, así que sin él dos máquinas
 * con el mismo puntaje se tratarían como una sola y la segunda no entraría al árbol. La clave
 * compuesta es única en toda la organización y nunca cambia, así que el mismo orden sirve para el
 * ranking de una compañía y para el global.</p>
 *
 * <p><strong>La trampa de la clave que cambia.</strong> El puntaje es la clave de orden. Si
 * cambiara con la máquina dentro del árbol, el árbol quedaría desordenado y {@code remove} dejaría
 * de encontrarla, sin lanzar ningún error. Por eso el puntaje solo cambia en
 * {@link #sumarFallo(Maquina, int)}, que primero saca la máquina de los dos rankings, luego suma y
 * luego la vuelve a insertar en ambos. {@link Maquina#sumarPuntaje(int)} tiene visibilidad de paquete para que nadie fuera de
 * {@code modelo} pueda saltarse este paso.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class RankingFallos {

    /** Puntaje de mayor a menor y, en empate, la clave compuesta (compañía, índice). */
    private static final Comparator<Maquina> ORDEN =
            Comparator.comparingInt(Maquina::puntajeFallos).reversed()
                    .thenComparingInt(Maquina::idCompania)
                    .thenComparingInt(Maquina::indice);

    /** Árbol rojo-negro con las máquinas, siempre en orden de ranking. */
    private final TreeSet<Maquina> arbol;

    /** Ranking global que se mantiene al día con este; {@code null} si no hay. */
    private final RankingFallos global;

    /**
     * Crea un ranking con las máquinas dadas, y las agrega también al global si se indica.
     *
     * <p>Para crear el ranking global se pasa una colección vacía y {@code null}: las compañías lo
     * van llenando al crear los suyos.</p>
     *
     * <p>Complejidad: O(n log n): n inserciones de O(log n).</p>
     *
     * @param maquinas máquinas iniciales
     * @param global   ranking global que se mantiene al día con este, o {@code null}
     * @throws NullPointerException si la colección es nula
     */
    public RankingFallos(Collection<Maquina> maquinas, RankingFallos global) {
        this.arbol = new TreeSet<>(ORDEN);
        this.global = global;
        for (Maquina maquina : Objects.requireNonNull(maquinas, "Las máquinas no pueden ser nulas")) {
            agregar(maquina);
        }
    }

    /**
     * Agrega una máquina a este ranking y al global.
     *
     * <p>Complejidad: O(log n).</p>
     *
     * @param maquina máquina a agregar
     */
    public void agregar(Maquina maquina) {
        arbol.add(maquina);
        if (global != null) {
            global.agregar(maquina);
        }
    }

    /**
     * Saca una máquina de este ranking y del global.
     *
     * <p>Hay que llamarlo <strong>antes</strong> de cambiar el puntaje de la máquina por cualquier
     * vía: con la clave ya cambiada, el árbol no la encontraría.</p>
     *
     * <p>Complejidad: O(log n).</p>
     *
     * @param maquina máquina a sacar; debe estar en el ranking
     * @throws IllegalStateException si la máquina no está en el ranking
     */
    public void quitar(Maquina maquina) {
        if (!arbol.remove(maquina)) {
            throw new IllegalStateException(
                    "La máquina " + maquina.etiqueta() + " no está en el ranking: "
                            + "su puntaje cambió por fuera de RankingFallos o nunca se insertó");
        }
        if (global != null) {
            global.quitar(maquina);
        }
    }

    /**
     * Suma un fallo a una máquina y la reubica en el ranking: <strong>eliminar → sumar →
     * insertar</strong>.
     *
     * <p>Es el único lugar donde cambia el puntaje de una máquina. La máquina sale de los dos
     * rankings, el de su compañía y el global, antes de que cambie su clave, y vuelve a entrar en
     * ambos después.</p>
     *
     * <p>Complejidad: O(log n): una eliminación y una inserción en cada árbol.</p>
     *
     * @param maquina máquina que falló; debe estar en el ranking
     * @param peso    puntos del fallo (ver {@link Prioridad#peso()})
     * @throws IllegalStateException si la máquina no está en el ranking
     */
    public void sumarFallo(Maquina maquina, int peso) {
        quitar(maquina);
        maquina.sumarPuntaje(peso);
        agregar(maquina);
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
