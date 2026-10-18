/**
 * Estructuras de datos implementadas por el equipo.
 *
 * <p>Aquí solo entra código genérico y reutilizable: las clases de este paquete
 * no conocen máquinas, sensores ni órdenes de trabajo. Se podrían copiar a otro
 * proyecto sin cambiar una línea.</p>
 *
 * <p>La única estructura propia del proyecto es la cola circular con
 * sobrescritura, porque la librería estándar de Java no ofrece ninguna: las
 * colas de {@code java.util} crecen o rechazan elementos cuando se llenan, pero
 * ninguna descarta el elemento más antiguo para dejar entrar el más nuevo, que
 * es justo el comportamiento de un registrador de datos industrial.</p>
 *
 * <p>Las demás estructuras del proyecto (arreglo dinámico, cola FIFO y pila) se
 * resuelven con {@code java.util}, tal como permite el enunciado.</p>
 */
package edu.unal.planta.estructuras;
