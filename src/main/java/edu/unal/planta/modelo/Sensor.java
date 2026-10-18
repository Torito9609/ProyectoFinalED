package edu.unal.planta.modelo;

import java.util.Objects;

/**
 * Definición de una variable que mide una máquina: qué es, en qué unidad, cuánto vale cuando todo
 * está bien y a partir de qué valores hay problema.
 *
 * <p>Es solo datos: el sensor no mide nada ni decide nada. El generador usa el valor nominal y el
 * ruido para producir lecturas, y el clasificador compara el valor leído contra los tres umbrales.</p>
 *
 * <p>El constructor exige dos condiciones, para que una configuración mal escrita falle de
 * inmediato en vez de arruinar la demostración:</p>
 * <ol>
 *   <li>Los umbrales van en orden creciente: advertencia &lt; alto &lt; crítico.</li>
 *   <li><strong>nominal + ruido &lt; umbral de advertencia.</strong> Sin este margen habría alarmas
 *       falsas por puro ruido, sin que exista ninguna anomalía.</li>
 * </ol>
 *
 * @param nombre            qué mide, por ejemplo {@code "Temperatura aceite"}
 * @param unidad            unidad de medida, por ejemplo {@code "°C"}
 * @param nominal           valor esperado con la máquina sana
 * @param ruido             desviación máxima del ruido normal, hacia arriba o hacia abajo
 * @param umbralAdvertencia a partir de aquí la lectura es sospechosa (prioridad P3)
 * @param umbralAlto        a partir de aquí la lectura es grave (prioridad P2)
 * @param umbralCritico     a partir de aquí la máquina debe detenerse (prioridad P1)
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record Sensor(
        String nombre,
        String unidad,
        double nominal,
        double ruido,
        double umbralAdvertencia,
        double umbralAlto,
        double umbralCritico) {

    /**
     * Valida la definición del sensor.
     *
     * @throws IllegalArgumentException si el ruido es negativo, si los umbrales no están en orden
     *                                 creciente o si no hay margen entre el ruido y la advertencia
     * @throws NullPointerException     si el nombre o la unidad son nulos
     */
    public Sensor {
        Objects.requireNonNull(nombre, "El sensor debe tener nombre");
        Objects.requireNonNull(unidad, "El sensor debe tener unidad");
        if (ruido < 0) {
            throw new IllegalArgumentException("El ruido no puede ser negativo: " + ruido);
        }
        if (umbralAdvertencia >= umbralAlto || umbralAlto >= umbralCritico) {
            throw new IllegalArgumentException("Los umbrales de " + nombre
                    + " deben ir en orden creciente, pero son advertencia=" + umbralAdvertencia
                    + ", alto=" + umbralAlto + ", crítico=" + umbralCritico);
        }
        if (nominal + ruido >= umbralAdvertencia) {
            throw new IllegalArgumentException("El sensor " + nombre + " daría alarmas falsas: "
                    + "nominal + ruido = " + (nominal + ruido)
                    + ", que no es menor que el umbral de advertencia " + umbralAdvertencia);
        }
    }

    /**
     * Indica si un valor leído alcanza el umbral de advertencia, es decir si hay que generar una
     * alarma.
     *
     * @param valor valor leído
     * @return {@code true} si el valor es anómalo
     */
    public boolean esAnomalo(double valor) {
        return valor >= umbralAdvertencia;
    }

    /**
     * Devuelve el valor formateado con su unidad, por ejemplo {@code "9,8 mm/s"}.
     *
     * @param valor valor leído
     * @return texto para la bitácora y los paneles
     */
    public String formatear(double valor) {
        return String.format("%.1f %s", valor, unidad);
    }
}
