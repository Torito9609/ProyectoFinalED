package edu.unal.planta.modelo;

import java.util.Objects;

/**
 * Fallo físico en curso en una máquina.
 *
 * <p>Representa la causa, no el síntoma: mientras una máquina tiene una anomalía activa, todas sus
 * lecturas del sensor afectado salen desplazadas, y así siguen hasta que un técnico atiende la
 * orden de trabajo correspondiente.</p>
 *
 * <p>Conviene distinguirla de {@link EventoFallo}, que es la <em>detección</em> de esa anomalía:
 * la anomalía puede existir un tick antes de que alguien la note, igual que en una planta real.</p>
 *
 * @param indiceSensor  posición del sensor afectado en la lista de sensores de la máquina
 * @param magnitud      gravedad del fallo
 * @param desplazamiento cuánto se le suma al valor nominal del sensor en cada lectura
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record Anomalia(int indiceSensor, Magnitud magnitud, double desplazamiento) {

    /**
     * Valida la anomalía.
     *
     * @throws IllegalArgumentException si el índice del sensor es negativo o el desplazamiento no
     *                                 es positivo
     * @throws NullPointerException     si la magnitud es nula
     */
    public Anomalia {
        Objects.requireNonNull(magnitud, "La anomalía debe tener magnitud");
        if (indiceSensor < 0) {
            throw new IllegalArgumentException("Índice de sensor negativo: " + indiceSensor);
        }
        if (desplazamiento <= 0) {
            throw new IllegalArgumentException(
                    "El desplazamiento debe ser positivo: " + desplazamiento);
        }
    }
}
