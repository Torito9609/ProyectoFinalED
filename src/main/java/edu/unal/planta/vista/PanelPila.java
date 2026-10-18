package edu.unal.planta.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JButton;

/**
 * Muestra la pila de acciones del operador, con la cima arriba.
 *
 * <p>Se dibuja en vertical y de arriba abajo porque así se entiende sin explicación: lo último que
 * se hizo queda encima y es lo primero que se revierte. Deshacer saca la caja de arriba.</p>
 *
 * <p>El panel recibe <strong>textos</strong>, no comandos. Si recibiera los objetos
 * {@code ComandoOperador} podría ejecutarlos o revertirlos por su cuenta, y la regla del proyecto es
 * que toda acción pase por el controlador.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelPila extends PanelEstructura {

    private static final int ALTO_CAJA = 26;
    private static final int ALTO_BOTON = 30;

    private final JButton botonDeshacer = new JButton("Deshacer");

    /** Prepara el panel con su botón de deshacer. */
    public PanelPila() {
        super("PILA — deshacer");
        setPreferredSize(new Dimension(210, 220));

        botonDeshacer.setFont(Colores.FUENTE_ETIQUETA);
        botonDeshacer.setToolTipText("Revierte la última acción del operador (la cima de la pila)");
        add(botonDeshacer, BorderLayout.SOUTH);
    }

    /**
     * Registra qué hacer cuando el operador pulse "Deshacer".
     *
     * @param accion acción a ejecutar
     */
    public void alDeshacer(Runnable accion) {
        for (var oyente : botonDeshacer.getActionListeners()) {
            botonDeshacer.removeActionListener(oyente);
        }
        botonDeshacer.addActionListener(evento -> accion.run());
    }

    @Override
    protected int altoReservadoAbajo() {
        return ALTO_BOTON;
    }

    @Override
    protected void actualizar() {
        // Sin acciones apiladas no hay nada que deshacer, y el botón lo dice.
        botonDeshacer.setEnabled(planta() != null && !planta().accionesDeshacibles().isEmpty());
    }

    @Override
    protected String subtitulo() {
        return planta() == null ? "" : planta().accionesDeshacibles().size() + " acciones";
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        List<String> acciones = planta().accionesDeshacibles();

        if (acciones.isEmpty()) {
            dibujarVacio(g, area, "pila vacía");
            return;
        }

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.RESALTADO);
        g.drawString("▲ cima: lo próximo en deshacerse", area.x, area.y + 9);

        int caben = Math.max(1, (area.height - 16) / ALTO_CAJA);
        for (int indice = 0; indice < Math.min(acciones.size(), caben); indice++) {
            Rectangle caja = new Rectangle(
                    area.x, area.y + 14 + indice * ALTO_CAJA, area.width - 2, ALTO_CAJA - 4);
            boolean esCima = indice == 0;
            dibujarCelda(g, caja,
                    esCima ? Colores.aclarar(Colores.RESALTADO, 0.85) : Colores.CELDA_VACIA,
                    esCima ? Colores.RESALTADO : Colores.BORDE,
                    recortar(acciones.get(indice), 26),
                    Colores.TEXTO,
                    esCima ? Colores.FUENTE_FUERTE : Colores.FUENTE_ETIQUETA);
        }

        if (acciones.size() > caben) {
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(Colores.TEXTO_SUAVE);
            g.drawString("+" + (acciones.size() - caben) + " debajo",
                    area.x + 2, area.y + 14 + caben * ALTO_CAJA + 9);
        }
    }

    /**
     * Recorta una descripción que no cabe en la caja.
     *
     * @param texto  texto original
     * @param maximo cuántos caracteres caben
     * @return el texto, abreviado si hizo falta
     */
    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
