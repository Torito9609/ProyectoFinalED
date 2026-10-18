package edu.unal.planta.vista;

import edu.unal.planta.estructuras.ColaCircularSoloLectura;
import edu.unal.planta.modelo.Lectura;
import edu.unal.planta.modelo.Maquina;
import edu.unal.planta.modelo.Sensor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * Muestra la cola circular de lecturas de la máquina seleccionada.
 *
 * <p>Es el único panel que dibuja el <strong>arreglo físico</strong> de una estructura, y puede
 * hacerlo porque {@code ColaCircular} es propia y expone sus celdas, sus punteros y su contador de
 * sobrescrituras. Se ven las celdas vacías, se ve qué celda ocupa el frente, y se ve el final dar
 * la vuelta cuando la cola se llena.</p>
 *
 * <p>Los paneles de las estructuras de {@code java.util} no pueden mostrar esto, y a propósito no lo
 * intentan: solo dibujan la secuencia de elementos que el tipo abstracto expone.</p>
 *
 * <p>Recibe la cola como {@link ColaCircularSoloLectura}, así que no tiene forma de modificarla
 * aunque quisiera.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelColaCircular extends PanelEstructura {

    private static final int ALTO_CELDA = 28;

    /** Prepara el panel. */
    public PanelColaCircular() {
        super("COLA CIRCULAR — lecturas recientes (propia)");
        setPreferredSize(new Dimension(640, 118));
    }

    @Override
    protected String subtitulo() {
        if (planta() == null) {
            return "";
        }
        ColaCircularSoloLectura<Lectura> cola = maquina().lecturas();
        return "tamaño " + cola.tamano() + "/" + cola.capacidad()
                + "   ·   sobrescrituras: " + cola.sobrescrituras();
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        Maquina maquina = maquina();
        Sensor sensor = maquina.sensor(indiceSensor());
        ColaCircularSoloLectura<Lectura> cola = maquina.lecturas();

        // Encabezado: de quién son estas lecturas.
        g.setFont(Colores.FUENTE_FUERTE);
        g.setColor(Colores.TEXTO);
        g.drawString(maquina.etiqueta() + "  ·  " + sensor.nombre() + " (" + sensor.unidad() + ")",
                area.x, area.y + 11);

        int capacidad = cola.capacidad();
        int anchoCelda = Math.max(10, area.width / capacidad);
        int yPunteros = area.y + 38;
        int yCeldas = yPunteros + 2;

        for (int indice = 0; indice < capacidad; indice++) {
            Rectangle celda = new Rectangle(
                    area.x + indice * anchoCelda, yCeldas, anchoCelda - 2, ALTO_CELDA);
            dibujarUnaCelda(g, celda, cola, sensor, indice);

            // Índice físico, debajo de la celda.
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(Colores.TEXTO_SUAVE);
            textoCentrado(g, String.valueOf(indice),
                    new Rectangle(celda.x, celda.y + ALTO_CELDA, celda.width, 12));
        }

        dibujarPunteros(g, area, cola, anchoCelda, yPunteros);
    }

    /**
     * Dibuja una celda del arreglo, vacía o con el valor del sensor seleccionado.
     *
     * @param g      lienzo
     * @param celda  posición y tamaño
     * @param cola   cola de lecturas
     * @param sensor sensor cuyo valor se muestra
     * @param indice posición física de la celda
     */
    private void dibujarUnaCelda(Graphics2D g, Rectangle celda,
                                 ColaCircularSoloLectura<Lectura> cola, Sensor sensor, int indice) {
        Optional<Lectura> contenido = cola.contenidoCelda(indice);
        if (contenido.isEmpty()) {
            dibujarCelda(g, celda, Colores.CELDA_VACIA, Colores.BORDE, "",
                    Colores.TEXTO_SUAVE, Colores.FUENTE_VALOR);
            return;
        }
        double valor = contenido.orElseThrow().valor(indiceSensor());
        Color zona = Colores.zonaDe(sensor, valor);
        dibujarCelda(g, celda, Colores.aclarar(zona, 0.85), zona,
                String.format("%.1f", valor), zona.darker(), Colores.FUENTE_VALOR);
    }

    /**
     * Dibuja los punteros de frente y final sobre las celdas que señalan.
     *
     * <p>Cuando la cola está llena los dos coinciden, porque
     * {@code (frente + capacidad) % capacidad} es igual a {@code frente}. En ese caso se dibuja un
     * solo puntero con los dos nombres, que es precisamente el momento en que la próxima lectura
     * sobrescribe la más antigua.</p>
     *
     * @param g          lienzo
     * @param area       zona disponible
     * @param cola       cola de lecturas
     * @param anchoCelda ancho de cada celda
     * @param y          coordenada vertical de la punta
     */
    private void dibujarPunteros(Graphics2D g, Rectangle area,
                                 ColaCircularSoloLectura<Lectura> cola, int anchoCelda, int y) {
        int centroFrente = area.x + cola.indiceFrente() * anchoCelda + (anchoCelda - 2) / 2;
        int centroFinal = area.x + cola.indiceFinal() * anchoCelda + (anchoCelda - 2) / 2;

        if (cola.indiceFrente() == cola.indiceFinal()) {
            dibujarPuntero(g, centroFrente, y, "frente/final", Colores.RESALTADO);
            return;
        }
        dibujarPuntero(g, centroFrente, y, "frente", Colores.ZONA_NORMAL);
        dibujarPuntero(g, centroFinal, y, "final", Colores.RESALTADO);
    }

    /**
     * Devuelve la máquina seleccionada.
     *
     * @return la máquina que se está mostrando
     */
    private Maquina maquina() {
        return planta().maquina(seleccion().indiceMaquina());
    }

    /**
     * Devuelve el sensor seleccionado, ajustado a los sensores que esa máquina realmente tiene.
     *
     * @return posición válida de sensor
     */
    private int indiceSensor() {
        return Math.min(seleccion().indiceSensor(), maquina().sensores().size() - 1);
    }
}
