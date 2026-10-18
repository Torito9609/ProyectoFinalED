package edu.unal.planta.vista;

import edu.unal.planta.modelo.EventoFallo;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Muestra la cola FIFO de eventos detectados que esperan clasificación.
 *
 * <p>Los eventos se dibujan de izquierda a derecha: el de la izquierda es el próximo en salir y por
 * la derecha entran los nuevos. Es la lectura directa de "primero en entrar, primero en salir".</p>
 *
 * <p>A diferencia del panel de la cola circular, aquí <strong>no</strong> se dibujan celdas vacías
 * ni capacidad. La cola es un {@code ArrayDeque} de {@code java.util}, que no expone su arreglo
 * interno; dibujar celdas sería inventar una memoria que no conocemos. Se muestra solo la secuencia
 * de elementos, que es lo que el tipo abstracto garantiza.</p>
 *
 * <p>Como solo se clasifica un evento por tick, aquí se ve la cola acumularse cuando varias
 * máquinas se alarman a la vez.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelColaEventos extends PanelEstructura {

    private static final int ANCHO_CAJA = 104;
    private static final int ALTO_CAJA = 38;

    /** Prepara el panel. */
    public PanelColaEventos() {
        super("COLA DE EVENTOS — ArrayDeque (FIFO)");
        setPreferredSize(new Dimension(420, 132));
    }

    @Override
    protected String subtitulo() {
        return planta() == null ? "" : planta().eventosPendientes().size() + " por clasificar";
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        List<EventoFallo> eventos = planta().eventosPendientes();

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.TEXTO_SUAVE);
        g.drawString("◄ sale (frente)", area.x, area.y + 9);
        String entra = "entra (final) ►";
        g.drawString(entra, area.x + area.width - g.getFontMetrics().stringWidth(entra), area.y + 9);

        Rectangle fila = new Rectangle(area.x, area.y + 16, area.width, ALTO_CAJA);
        if (eventos.isEmpty()) {
            dibujarVacio(g, fila, "cola vacía: ningún evento sin clasificar");
            return;
        }

        int caben = Math.max(1, area.width / ANCHO_CAJA);
        for (int indice = 0; indice < Math.min(eventos.size(), caben); indice++) {
            dibujarEvento(g,
                    new Rectangle(area.x + indice * ANCHO_CAJA, fila.y, ANCHO_CAJA - 6, ALTO_CAJA),
                    eventos.get(indice), indice == 0);
        }
        if (eventos.size() > caben) {
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(Colores.TEXTO_SUAVE);
            g.drawString("+" + (eventos.size() - caben), area.x + caben * ANCHO_CAJA, fila.y + 22);
        }
    }

    /**
     * Dibuja la caja de un evento.
     *
     * @param g       lienzo
     * @param caja    posición y tamaño
     * @param evento  evento a dibujar
     * @param esFrente si es el próximo en salir
     */
    private void dibujarEvento(Graphics2D g, Rectangle caja, EventoFallo evento, boolean esFrente) {
        g.setColor(esFrente ? Colores.aclarar(Colores.RESALTADO, 0.88) : Colores.CELDA_VACIA);
        g.fillRect(caja.x, caja.y, caja.width, caja.height);
        g.setColor(esFrente ? Colores.RESALTADO : Colores.BORDE);
        g.drawRect(caja.x, caja.y, caja.width, caja.height);

        g.setFont(Colores.FUENTE_FUERTE);
        g.setColor(Colores.TEXTO);
        g.drawString(evento.etiquetaMaquina(), caja.x + 6, caja.y + 14);

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.TEXTO_SUAVE);
        g.drawString(recortar(evento.nombreSensor(), 16), caja.x + 6, caja.y + 25);
        g.drawString("tick " + evento.tick(), caja.x + 6, caja.y + 34);
    }

    /**
     * Recorta un texto que no cabe en la caja.
     *
     * @param texto  texto original
     * @param maximo cuántos caracteres caben
     * @return el texto, abreviado con puntos suspensivos si hizo falta
     */
    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
