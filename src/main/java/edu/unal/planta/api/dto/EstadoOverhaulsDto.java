package edu.unal.planta.api.dto;

import java.util.List;

/**
 * Foto del montículo de solicitudes de overhaul y de los técnicos del fabricante.
 *
 * <p>El montículo es un {@code PriorityQueue}, que no expone su arreglo interno: el orden de
 * {@code toArray()} no está garantizado. Por eso no se envía el árbol, sino la <strong>cima</strong>
 * ({@code peek}, O(1)) y el <strong>orden de salida</strong>, calculado extrayendo de una copia
 * (O(s log s), con s pequeño).</p>
 *
 * @param modo             {@code AUTOMATICO} o {@code MANUAL}; el botón de despachar solo
 *                         funciona en manual
 * @param cima             la solicitud que saldrá primero; {@code null} si el montículo está vacío
 * @param ordenDeSalida    las solicitudes, de la primera a la última en salir
 * @param tecnicos         todos los técnicos del fabricante, libres u ocupados
 * @param tecnicosLibres   nombres de los técnicos libres, en el orden de su cola FIFO
 * @param duracionOverhaul ticks que dura un overhaul
 * @param costoOverhaul    lo que cuesta despachar el camión del fabricante
 * @param costoReemplazo   lo que cuesta reemplazar una máquina
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EstadoOverhaulsDto(
        String modo,
        SolicitudOverhaulDto cima,
        List<SolicitudOverhaulDto> ordenDeSalida,
        List<TecnicoExternoDto> tecnicos,
        List<String> tecnicosLibres,
        int duracionOverhaul,
        long costoOverhaul,
        long costoReemplazo) {
}
