package edu.unal.planta.vista;

import edu.unal.planta.modelo.Maquina;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Muestra el inventario de máquinas, que es un {@code ArrayList}.
 *
 * <p>Dibuja una fila por posición, con su índice a la izquierda, porque lo característico de un
 * arreglo dinámico es justamente el acceso por índice en O(1). Es también el índice que guardan las
 * órdenes de trabajo para recuperar su máquina.</p>
 *
 * <p>No se dibuja la capacidad interna del {@code ArrayList}: Java no la expone y inventarla sería
 * mostrar una memoria que no sabemos si existe. Se muestra el comportamiento del tipo abstracto,
 * no una suposición.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelInventario extends PanelEstructura {

    private static final int ALTO_FILA = 32;
    private static final int ANCHO_INDICE = 24;

    /** Prepara el panel. */
    public PanelInventario() {
        super("INVENTARIO — ArrayList");
        setPreferredSize(new Dimension(250, 6 * ALTO_FILA + ALTO_CABECERA + 2 * MARGEN));
    }

    @Override
    protected String subtitulo() {
        return planta() == null ? "" : "tamaño " + planta().inventario().size();
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        List<Maquina> inventario = planta().inventario();

        for (int indice = 0; indice < inventario.size(); indice++) {
            Maquina maquina = inventario.get(indice);
            boolean seleccionada = indice == seleccion().indiceMaquina();
            int y = area.y + indice * ALTO_FILA;

            dibujarIndice(g, area.x, y, indice, seleccionada);
            dibujarMaquina(g, new Rectangle(
                    area.x + ANCHO_INDICE + 4, y,
                    area.width - ANCHO_INDICE - 4, ALTO_FILA - 5), maquina, seleccionada);
        }
    }

    /**
     * Dibuja la casilla del índice del arreglo.
     *
     * @param g           lienzo
     * @param x           coordenada horizontal
     * @param y           coordenada vertical
     * @param indice      posición en el arreglo
     * @param seleccionada si es la máquina que el operador está mirando
     */
    private void dibujarIndice(Graphics2D g, int x, int y, int indice, boolean seleccionada) {
        Rectangle casilla = new Rectangle(x, y, ANCHO_INDICE, ALTO_FILA - 5);
        dibujarCelda(g, casilla,
                seleccionada ? Colores.RESALTADO : Colores.CELDA_VACIA,
                Colores.BORDE,
                String.valueOf(indice),
                seleccionada ? Color.WHITE : Colores.TEXTO_SUAVE,
                Colores.FUENTE_VALOR);
    }

    /**
     * Dibuja la ficha de una máquina, con una franja del color de su estado.
     *
     * @param g           lienzo
     * @param caja        posición y tamaño
     * @param maquina     máquina a dibujar
     * @param seleccionada si es la que el operador está mirando
     */
    private void dibujarMaquina(Graphics2D g, Rectangle caja, Maquina maquina,
                                boolean seleccionada) {
        Color estado = Colores.de(maquina.estado().rgb());

        g.setColor(Colores.aclarar(estado, 0.90));
        g.fillRect(caja.x, caja.y, caja.width, caja.height);
        g.setColor(seleccionada ? Colores.RESALTADO : Colores.BORDE);
        g.drawRect(caja.x, caja.y, caja.width, caja.height);
        g.setColor(estado);
        g.fillRect(caja.x + 1, caja.y + 1, 4, caja.height - 1);

        g.setFont(Colores.FUENTE_FUERTE);
        g.setColor(Colores.TEXTO);
        g.drawString(maquina.etiqueta(), caja.x + 11, caja.y + 13);

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(estado.darker());
        String detalle = maquina.estado().descripcion() + "  ·  carga " + maquina.carga() + "%";
        if (maquina.alarmaActiva()) {
            detalle += "  ·  ALARMA";
        }
        g.drawString(detalle, caja.x + 11, caja.y + 24);
    }
}
