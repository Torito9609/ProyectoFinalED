/**
 * Estructuras de datos implementadas por el equipo.
 *
 * <p>Aquí solo entra código genérico y reutilizable: las clases de este paquete
 * no conocen máquinas, sensores ni órdenes de trabajo. Se podrían copiar a otro
 * proyecto sin cambiar una línea.</p>
 *
 * <p>Solo son propias las estructuras que la librería estándar de Java no ofrece:</p>
 * <ul>
 *   <li>{@link edu.unal.planta.estructuras.ColaCircular} (Entrega 1): las colas de
 *   {@code java.util} crecen o rechazan elementos cuando se llenan, pero ninguna
 *   descarta el elemento más antiguo para dejar entrar el más nuevo, que es justo
 *   el comportamiento de un registrador de datos industrial.</li>
 *   <li>{@link edu.unal.planta.estructuras.ArbolSegmentos} (Entrega 2): responde la
 *   suma y el máximo de un rango de ticks en O(log n).</li>
 * </ul>
 *
 * <p>Las demás estructuras del proyecto (arreglo dinámico, cola FIFO, pila, árbol
 * rojo-negro y montículo) se resuelven con {@code java.util}, tal como permite el
 * enunciado.</p>
 */
package edu.unal.planta.estructuras;
