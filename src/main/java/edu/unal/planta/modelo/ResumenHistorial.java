package edu.unal.planta.modelo;

/**
 * Lo que responde el historial para una ventana de ticks {@code [desde, hasta]}: el total y el pico
 * de las dos series, el puntaje de fallos y el costo.
 *
 * <p>Los totales salen de la <strong>suma</strong> del árbol de segmentos y los picos de su
 * <strong>máximo</strong>, cada uno en O(log n). El pico es lo que justifica el árbol: un arreglo de
 * sumas prefijas daría los totales, pero no el máximo de un rango arbitrario.</p>
 *
 * @param totalFallos puntaje de fallos sumado en la ventana
 * @param picoFallos  mayor puntaje de fallos de un solo tick en la ventana
 * @param totalCosto  costo sumado en la ventana
 * @param picoCosto   mayor costo de un solo tick en la ventana
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ResumenHistorial(long totalFallos, int picoFallos, long totalCosto, int picoCosto) {
}
