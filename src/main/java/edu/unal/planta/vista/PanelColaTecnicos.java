package edu.unal.planta.vista;

import edu.unal.planta.modelo.OrdenTrabajo;
import edu.unal.planta.modelo.Tecnico;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Muestra la cola FIFO de técnicos disponibles y, aparte, los que están trabajando.
 *
 * <p>La fila de arriba es la cola: el técnico de la izquierda lleva más tiempo esperando y es el
 * próximo en recibir orden. Cuando un técnico termina vuelve <strong>al final</strong> de esa fila,
 * y por eso el trabajo se reparte parejo entre los dos.</p>
 *
 * <p>Los que están ocupados no pertenecen a la cola: se dibujan debajo, con su orden y los ticks
 * que le faltan.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelColaTecnicos extends PanelEstructura {

    private static final int ANCHO_CAJA = 86;
    private static final int ALTO_CAJA = 26;

    /** Prepara el panel. */
    public PanelColaTecnicos() {
        super("COLA DE TÉCNICOS — ArrayDeque (FIFO)");
        setPreferredSize(new Dimension(360, 132));
    }

    @Override
    protected String subtitulo() {
        if (planta() == null) {
            return "";
        }
        return planta().tecnicosLibres().size() + " libres de " + planta().tecnicos().size();
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        dibujarLibres(g, area);
        dibujarOcupados(g, area);
    }

    /**
     * Dibuja la cola de técnicos disponibles.
     *
     * @param g    lienzo
     * @param area zona disponible
     */
    private void dibujarLibres(Graphics2D g, Rectangle area) {
        List<Tecnico> libres = planta().tecnicosLibres();

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.TEXTO_SUAVE);
        g.drawString("◄ sale   disponibles   entra ►", area.x, area.y + 9);

        Rectangle fila = new Rectangle(area.x, area.y + 14, area.width, ALTO_CAJA);
        if (libres.isEmpty()) {
            g.setFont(Colores.FUENTE_ETIQUETA);
            g.setColor(Colores.TEXTO_SUAVE);
            g.drawString("ninguno libre", area.x + 2, fila.y + 17);
            return;
        }
        for (int indice = 0; indice < libres.size(); indice++) {
            Rectangle caja = new Rectangle(
                    area.x + indice * ANCHO_CAJA, fila.y, ANCHO_CAJA - 6, ALTO_CAJA);
            dibujarCelda(g, caja,
                    indice == 0 ? Colores.aclarar(Colores.ZONA_NORMAL, 0.85) : Colores.CELDA_VACIA,
                    indice == 0 ? Colores.ZONA_NORMAL : Colores.BORDE,
                    libres.get(indice).nombre(), Colores.TEXTO, Colores.FUENTE_FUERTE);
        }
    }

    /**
     * Dibuja los técnicos ocupados, con su orden y los ticks restantes.
     *
     * @param g    lienzo
     * @param area zona disponible
     */
    private void dibujarOcupados(Graphics2D g, Rectangle area) {
        int y = area.y + 50;
        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.TEXTO_SUAVE);
        g.drawString("trabajando", area.x, y);

        int fila = 0;
        for (Tecnico tecnico : planta().tecnicos()) {
            if (tecnico.estaLibre()) {
                continue;
            }
            OrdenTrabajo orden = tecnico.ordenActual().orElseThrow();
            Color color = Colores.de(orden.prioridad().rgb());
            int yFila = y + 11 + fila * 13;

            g.setFont(Colores.FUENTE_ETIQUETA);
            g.setColor(Colores.TEXTO);
            g.drawString(tecnico.nombre() + " → " + orden.etiquetaCorta(), area.x + 2, yFila);

            String restante = orden.prioridad().name() + " · " + tecnico.ticksRestantes() + " ticks";
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(color.darker());
            g.drawString(restante,
                    area.x + area.width - g.getFontMetrics().stringWidth(restante), yFila);
            fila++;
        }
        if (fila == 0) {
            g.setFont(Colores.FUENTE_ETIQUETA);
            g.setColor(Colores.TEXTO_SUAVE);
            g.drawString("nadie trabajando", area.x + 2, y + 13);
        }
    }
}
