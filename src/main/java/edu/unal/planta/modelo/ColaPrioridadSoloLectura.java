package edu.unal.planta.modelo;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones de consulta de la cola con prioridad, sin ninguna que modifique su contenido.
 *
 * <p>Cumple el mismo papel que {@link edu.unal.planta.estructuras.ColaCircularSoloLectura} y por la
 * misma razón: el panel de los cuatro carriles necesita ver el interior de la cola, pero no debe
 * poder tocarlo. Si recibiera la {@link ColaPrioridadOrdenes} completa, nada le impediría llamar a
 * {@code extraer()} y sacar una orden que nadie va a atender.</p>
 *
 * <p>Nótese que aquí falta justamente lo que modifica: {@code insertar}, {@code extraer} y
 * {@code vaciar}. Eso es todo lo que hace falta para que el compilador garantice la regla.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public interface ColaPrioridadSoloLectura {

    /**
     * Devuelve la próxima orden en salir, sin extraerla.
     *
     * @return la siguiente orden, o un {@link Optional} vacío si no hay órdenes pendientes
     */
    Optional<OrdenTrabajo> verSiguiente();

    /**
     * Devuelve el nivel del que saldrá la próxima orden.
     *
     * @return la prioridad del próximo en salir, o un {@link Optional} vacío si la cola está vacía
     */
    Optional<Prioridad> nivelDelSiguiente();

    /**
     * Devuelve el total de órdenes pendientes en los cuatro niveles.
     *
     * @return cantidad total de órdenes pendientes
     */
    int tamano();

    /**
     * Devuelve cuántas órdenes hay pendientes en un nivel.
     *
     * @param prioridad nivel consultado
     * @return cantidad de órdenes en ese nivel
     */
    int tamanoNivel(Prioridad prioridad);

    /**
     * Indica si no hay ninguna orden pendiente.
     *
     * @return {@code true} si los cuatro niveles están vacíos
     */
    boolean estaVacia();

    /**
     * Devuelve las órdenes de un nivel, de la primera a la última en salir.
     *
     * @param prioridad nivel consultado
     * @return lista no modificable con las órdenes del nivel, en orden FIFO
     */
    List<OrdenTrabajo> ordenesDe(Prioridad prioridad);
}
