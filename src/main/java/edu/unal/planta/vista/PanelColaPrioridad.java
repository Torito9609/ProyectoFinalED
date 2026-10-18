package edu.unal.planta.vista;

import edu.unal.planta.modelo.ColaPrioridadSoloLectura;
import edu.unal.planta.modelo.OrdenTrabajo;
import edu.unal.planta.modelo.Prioridad;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

/**
 * Muestra la cola con prioridad como lo que realmente es: <strong>cuatro colas FIFO</strong>, una
 * por nivel.
 *
 * <p>Este panel es la explicación visual de la estructura. Cada carril es una cola independiente, y
 * se ve que dentro de un carril las órdenes salen en orden de llegada, mientras que entre carriles
 * manda la prioridad. Una P1 que llega de última aparece en el carril de arriba y se lleva el turno:
 * eso es exactamente lo que {@code PriorityQueue} no garantiza y la razón de esta implementación.</p>
 *
 * <p>El carril del que sale la próxima orden se resalta con un marco y una flecha.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelColaPrioridad extends PanelEstructura {

    private static final int ALTO_CARRIL = 28;
    private static final int ANCHO_ETIQUETA = 92;
    private static final int ANCHO_ORDEN = 96;

    /** Prepara el panel. */
    public PanelColaPrioridad() {
        super("COLA CON PRIORIDAD — arreglo de 4 colas FIFO");
        setPreferredSize(new Dimension(640, 4 * ALTO_CARRIL + ALTO_CABECERA + 2 * MARGEN + 6));
    }

    @Override
    protected String subtitulo() {
        if (planta() == null) {
            return "";
        }
        Optional<Prioridad> siguiente = planta().ordenes().nivelDelSiguiente();
        String activo = siguiente.map(p -> "   ·   sale de " + p.name()).orElse("");
        return planta().ordenes().tamano() + " órdenes" + activo;
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        ColaPrioridadSoloLectura ordenes = planta().ordenes();
        Optional<Prioridad> activo = ordenes.nivelDelSiguiente();

        for (Prioridad prioridad : Prioridad.values()) {
            int y = area.y + prioridad.nivel() * ALTO_CARRIL;
            boolean esActivo = activo.isPresent() && activo.get() == prioridad;
            dibujarCarril(g, area, y, prioridad, ordenes.ordenesDe(prioridad), esActivo);
        }
    }

    /**
     * Dibuja un carril completo: su etiqueta y las órdenes que contiene.
     *
     * @param g         lienzo
     * @param area      zona disponible
     * @param y         coordenada vertical del carril
     * @param prioridad nivel del carril
     * @param ordenes   órdenes de ese nivel, en orden de salida
     * @param esActivo  si de este carril sale la próxima orden
     */
    private void dibujarCarril(Graphics2D g, Rectangle area, int y, Prioridad prioridad,
                               List<OrdenTrabajo> ordenes, boolean esActivo) {
        Color color = Colores.de(prioridad.rgb());
        int alto = ALTO_CARRIL - 5;

        // Etiqueta del nivel, con su color.
        Rectangle etiqueta = new Rectangle(area.x, y, ANCHO_ETIQUETA, alto);
        dibujarCelda(g, etiqueta, color, color.darker(),
                prioridad.etiqueta() + " (" + ordenes.size() + ")",
                Color.WHITE, Colores.FUENTE_FUERTE);

        int xOrdenes = area.x + ANCHO_ETIQUETA + 10;
        int anchoDisponible = area.width - ANCHO_ETIQUETA - 10;

        if (esActivo) {
            dibujarFlechaDeSalida(g, area.x + ANCHO_ETIQUETA + 2, y + alto / 2);
        }

        if (ordenes.isEmpty()) {
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(Colores.TEXTO_SUAVE);
            g.drawString("carril vacío", xOrdenes + 2, y + alto / 2 + 4);
            return;
        }

        int caben = Math.max(1, anchoDisponible / ANCHO_ORDEN);
        for (int indice = 0; indice < Math.min(ordenes.size(), caben); indice++) {
            Rectangle caja = new Rectangle(
                    xOrdenes + indice * ANCHO_ORDEN, y, ANCHO_ORDEN - 6, alto);
            boolean primera = indice == 0 && esActivo;
            dibujarCelda(g, caja,
                    Colores.aclarar(color, primera ? 0.75 : 0.90),
                    primera ? color.darker() : color,
                    ordenes.get(indice).etiquetaCorta(),
                    Colores.TEXTO, Colores.FUENTE_PEQUENA);
        }
        if (ordenes.size() > caben) {
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(Colores.TEXTO_SUAVE);
            g.drawString("+" + (ordenes.size() - caben),
                    xOrdenes + caben * ANCHO_ORDEN, y + alto / 2 + 4);
        }
    }

    /**
     * Dibuja la flecha que marca el carril del que sale la próxima orden.
     *
     * @param g lienzo
     * @param x coordenada horizontal de la punta
     * @param y coordenada vertical del centro
     */
    private void dibujarFlechaDeSalida(Graphics2D g, int x, int y) {
        g.setColor(Colores.TEXTO);
        g.fillPolygon(new int[] {x, x + 7, x}, new int[] {y - 5, y, y + 5}, 3);
    }
}
