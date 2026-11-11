package edu.unal.planta.api.dto;

import java.util.List;

/**
 * Historial de una ventana de ticks {@code [desde, hasta]}: el total y el pico de las dos series
 * (puntaje de fallos y costo), y el valor de cada tick para dibujar la gráfica.
 *
 * <p>Los totales y los picos salen de la suma y el máximo de rango del árbol de segmentos, en
 * O(log n) cada uno. Las series se leen hoja por hoja, en O(1) por tick.</p>
 *
 * @param alcance       {@code "Global"} o el nombre de la compañía
 * @param desde         primer tick de la ventana
 * @param hasta         último tick de la ventana, incluido
 * @param totalFallos   puntaje de fallos sumado en la ventana
 * @param picoFallos    mayor puntaje de fallos de un solo tick en la ventana
 * @param totalCosto    costo sumado en la ventana
 * @param picoCosto     mayor costo de un solo tick en la ventana
 * @param fallosPorTick puntaje de fallos de cada tick, de {@code desde} a {@code hasta}
 * @param costoPorTick  costo de cada tick, de {@code desde} a {@code hasta}
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record HistorialDto(
        String alcance,
        int desde,
        int hasta,
        long totalFallos,
        int picoFallos,
        long totalCosto,
        int picoCosto,
        List<Integer> fallosPorTick,
        List<Integer> costoPorTick) {
}
