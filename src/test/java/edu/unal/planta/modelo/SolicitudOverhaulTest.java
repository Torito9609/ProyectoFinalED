package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del orden del montículo de solicitudes de overhaul.
 */
@DisplayName("SolicitudOverhaul")
class SolicitudOverhaulTest {

    @Test
    @DisplayName("sale primero el mayor puntaje, y en un empate la que llegó antes")
    void mayorPuntajeYOrdenDeLlegada() {
        PriorityQueue<SolicitudOverhaul> monticulo = new PriorityQueue<>(SolicitudOverhaul.COMPARADOR);
        monticulo.add(solicitud("A", 10, 1));
        monticulo.add(solicitud("B", 20, 2));
        monticulo.add(solicitud("C", 10, 3));
        monticulo.add(solicitud("D", 20, 4));

        List<String> salida = new ArrayList<>();
        while (!monticulo.isEmpty()) {
            salida.add(monticulo.poll().etiqueta());
        }

        assertEquals(List.of("B", "D", "A", "C"), salida);
    }

    private static SolicitudOverhaul solicitud(String etiqueta, int puntaje, long secuencia) {
        return new SolicitudOverhaul(0, 0, etiqueta, puntaje, secuencia, 0);
    }
}
