package edu.unal.planta.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La organización: varias compañías, cada una con su propia {@link Planta}, y el ranking global
 * de fallos de todas ellas.
 *
 * <h2>Estructuras</h2>
 * <ul>
 *   <li><strong>Arreglo dinámico</strong> ({@code ArrayList<Planta>}): las compañías. El id de una
 *   compañía es su índice, así que buscarla por id cuesta O(1). El tick las recorre todas en
 *   orden, O(número de compañías). Las compañías no se eliminan, así que los ids no se corren.</li>
 *   <li><strong>Árbol rojo-negro</strong> ({@link RankingFallos}): el ranking global, con las
 *   máquinas de todas las compañías. Cada compañía lo mantiene al día con las suyas.</li>
 * </ul>
 *
 * <p>La jerarquía organización → compañías → máquinas tiene profundidad fija, por eso basta una
 * lista y no hace falta un árbol n-ario (ver la "Opción B" del documento de diseño).</p>
 *
 * <h2>Aleatoriedad reproducible</h2>
 * <p>La organización recibe una <strong>semilla maestra</strong>, y la semilla de cada compañía
 * se deriva de ella y de su id con {@link #semillaCompania(long, int)}. La misma semilla maestra
 * produce la misma simulación en todas las compañías.</p>
 *
 * <p>Como {@link Planta}, no es segura para hilos: el servicio serializa el acceso.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Organizacion {

    /**
     * Separación entre las semillas de compañías vecinas: la compañía {@code i} usa
     * {@code maestra × 1000 + i}.
     */
    private static final long FACTOR_SEMILLA = 1000L;

    private final long semillaMaestra;

    /** Arreglo dinámico: las compañías; el id de cada una es su índice. */
    private final List<Planta> companias;

    /** Árbol rojo-negro: las máquinas de todas las compañías, por puntaje de fallos. */
    private final RankingFallos rankingGlobal;

    private int tick;

    /**
     * Crea la organización con las compañías de la configuración, listas para el primer tick.
     *
     * <p>Complejidad: O(N log N), con N el total de máquinas, por las inserciones en el ranking
     * global.</p>
     *
     * @param semillaMaestra semilla de la que se derivan las de todas las compañías
     */
    public Organizacion(long semillaMaestra) {
        this.semillaMaestra = semillaMaestra;
        this.rankingGlobal = new RankingFallos(List.of(), null);
        this.companias = new ArrayList<>();
        for (String nombre : ConfiguracionPlanta.NOMBRES_COMPANIAS) {
            int id = companias.size();
            companias.add(new Planta(id, nombre, semillaCompania(semillaMaestra, id), rankingGlobal));
        }
        this.tick = 0;
    }

    /**
     * Deriva la semilla de una compañía a partir de la maestra y del id de la compañía.
     *
     * <p>La regla es {@code maestra × 1000 + id}: con la maestra 7, las compañías usan 7000, 7001,
     * 7002, etc. Es determinista y solo depende del id, así que una compañía creada después tendrá
     * siempre la misma semilla.</p>
     *
     * @param semillaMaestra semilla de la organización
     * @param idCompania     id de la compañía
     * @return semilla de esa compañía
     */
    public static long semillaCompania(long semillaMaestra, int idCompania) {
        return semillaMaestra * FACTOR_SEMILLA + idCompania;
    }

    /**
     * Avanza un tick en toda la organización: ejecuta {@link Planta#tick()} en cada compañía, en el
     * orden de sus ids, e incrementa el tick de la organización.
     *
     * <p>Los fallos que clasifique cada planta actualizan su ranking y el global.</p>
     */
    public void tick() {
        for (Planta compania : companias) {
            compania.tick();
        }
        tick++;
    }

    /**
     * Devuelve todas las compañías a su estado inicial, con las mismas semillas.
     *
     * <p>Cada planta saca sus máquinas del ranking global antes de reiniciarlas y las vuelve a
     * meter después, así que el ranking global queda consistente sin rearmarlo.</p>
     */
    public void reiniciar() {
        for (Planta compania : companias) {
            compania.reiniciar();
        }
        tick = 0;
    }

    /**
     * Devuelve las compañías, en el orden de sus ids.
     *
     * <p>Complejidad: O(1): es una vista no modificable, no una copia.</p>
     *
     * @return lista no modificable de compañías
     */
    public List<Planta> companias() {
        return Collections.unmodifiableList(companias);
    }

    /**
     * Devuelve una compañía por su id.
     *
     * <p>Complejidad: O(1): el id es el índice en el arreglo.</p>
     *
     * @param id id de la compañía
     * @return la compañía con ese id
     * @throws IndexOutOfBoundsException si no existe una compañía con ese id
     */
    public Planta compania(int id) {
        return companias.get(id);
    }

    /**
     * Devuelve las máquinas de todas las compañías, de la que tiene más fallos a la que tiene menos.
     *
     * <p>Complejidad: O(N), con N el total de máquinas: un recorrido en orden del árbol.</p>
     *
     * @return copia no modificable, en orden de ranking
     */
    public List<Maquina> rankingGlobal() {
        return rankingGlobal.enOrden();
    }

    /**
     * Devuelve el tick actual de la organización.
     *
     * @return número de ticks ejecutados desde el inicio o el último reinicio
     */
    public int tickActual() {
        return tick;
    }

    /**
     * Devuelve la semilla maestra.
     *
     * @return semilla de la que se derivan las de las compañías
     */
    public long semillaMaestra() {
        return semillaMaestra;
    }
}
