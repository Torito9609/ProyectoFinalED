package edu.unal.planta.vista;

import edu.unal.planta.modelo.Planta;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Base de todos los paneles que muestran una estructura de datos.
 *
 * <p>Resuelve tres cosas de una vez para sus siete subclases: el aspecto común (fondo, borde y
 * cabecera con título), el contrato de refresco, y los dibujos que todas repiten, como una celda
 * con texto centrado o un puntero.</p>
 *
 * <p>Usa el patrón <strong>método plantilla</strong>: {@link #paintComponent(Graphics)} está
 * resuelto aquí y pinta la cabecera, y lo único que cada subclase define es
 * {@link #dibujarContenido(Graphics2D, Rectangle)}. Así los siete paneles se ven como una familia
 * sin que ninguno repita código de presentación.</p>
 *
 * <p>Gracias a esta base, la ventana refresca todo con una sola llamada polimórfica sobre una lista
 * de {@code PanelEstructura}, sin saber qué panel es cuál.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public abstract class PanelEstructura extends JPanel {

    /** Margen interior de los paneles. */
    protected static final int MARGEN = 8;

    /** Alto que la cabecera reserva en la parte superior. */
    protected static final int ALTO_CABECERA = 22;

    private final String titulo;
    private Planta planta;
    private SeleccionVista seleccion = SeleccionVista.INICIAL;

    /**
     * Prepara el panel con su título.
     *
     * @param titulo nombre de la estructura, tal como aparece en la cabecera
     */
    protected PanelEstructura(String titulo) {
        this.titulo = titulo;
        setLayout(new BorderLayout());
        setBackground(Colores.PANEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colores.BORDE),
                BorderFactory.createEmptyBorder(ALTO_CABECERA, MARGEN, MARGEN, MARGEN)));
    }

    /**
     * Recibe el estado nuevo y pide repintar.
     *
     * <p>Es final a propósito: ningún panel debe cambiar cómo se refresca, solo qué dibuja.</p>
     *
     * @param planta    planta que hay que mostrar
     * @param seleccion qué está mirando el operador
     */
    public final void refrescar(Planta planta, SeleccionVista seleccion) {
        this.planta = planta;
        this.seleccion = seleccion;
        actualizar();
        repaint();
    }

    /**
     * Gancho para los paneles que, en vez de dibujar, usan componentes de Swing y necesitan
     * actualizarlos antes de repintar. Por omisión no hace nada.
     */
    protected void actualizar() {
        // Los paneles que dibujan no necesitan hacer nada aquí.
    }

    /**
     * Devuelve la planta que se está mostrando.
     *
     * @return la planta, o {@code null} si todavía no se ha refrescado
     */
    protected final Planta planta() {
        return planta;
    }

    /**
     * Devuelve lo que el operador tiene seleccionado.
     *
     * @return la selección actual
     */
    protected final SeleccionVista seleccion() {
        return seleccion;
    }

    /**
     * Texto que se muestra a la derecha de la cabecera. Por omisión no hay ninguno.
     *
     * @return texto corto, por ejemplo {@code "sobrescrituras: 36"}
     */
    protected String subtitulo() {
        return "";
    }

    /**
     * Dibuja el contenido del panel. Es lo único que cada subclase tiene que definir.
     *
     * @param g    lienzo, ya con suavizado activado
     * @param area zona disponible, por debajo de la cabecera y dentro de los márgenes
     */
    protected abstract void dibujarContenido(Graphics2D g, Rectangle area);

    @Override
    protected final void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        dibujarCabecera(g2);
        if (planta != null) {
            dibujarContenido(g2, areaDeContenido());
        }
        g2.dispose();
    }

    /**
     * Dibuja el título a la izquierda y el subtítulo a la derecha, con una línea divisoria.
     *
     * @param g lienzo
     */
    private void dibujarCabecera(Graphics2D g) {
        g.setFont(Colores.FUENTE_TITULO);
        g.setColor(Colores.TEXTO);
        g.drawString(titulo, MARGEN, 15);

        String derecha = subtitulo();
        if (!derecha.isEmpty()) {
            g.setFont(Colores.FUENTE_PEQUENA);
            g.setColor(Colores.TEXTO_SUAVE);
            int ancho = g.getFontMetrics().stringWidth(derecha);
            g.drawString(derecha, getWidth() - MARGEN - ancho, 15);
        }

        g.setColor(Colores.BORDE);
        g.drawLine(1, ALTO_CABECERA - 3, getWidth() - 2, ALTO_CABECERA - 3);
    }

    /**
     * Calcula la zona utilizable, descontando el borde y la cabecera.
     *
     * @return rectángulo donde las subclases pueden dibujar
     */
    private Rectangle areaDeContenido() {
        Insets margen = getInsets();
        return new Rectangle(
                margen.left,
                margen.top,
                Math.max(0, getWidth() - margen.left - margen.right),
                Math.max(0, getHeight() - margen.top - margen.bottom - altoReservadoAbajo()));
    }

    /**
     * Alto que el panel reserva en la parte inferior para componentes de Swing, por ejemplo un
     * botón. Por omisión no reserva nada.
     *
     * <p>Lo necesitan los paneles que mezclan dibujo y componentes: sin esta reserva, el dibujo
     * pasaría por debajo del botón.</p>
     *
     * @return alto en píxeles que no se debe dibujar
     */
    protected int altoReservadoAbajo() {
        return 0;
    }

    // ---------------------------------------------------------------------------------
    // Dibujos que comparten las subclases
    // ---------------------------------------------------------------------------------

    /**
     * Dibuja una celda con su texto centrado.
     *
     * @param g       lienzo
     * @param celda   posición y tamaño
     * @param relleno color de fondo
     * @param borde   color del borde
     * @param texto   texto a centrar; si es vacío, la celda queda en blanco
     * @param color   color del texto
     * @param fuente  tipografía del texto
     */
    protected void dibujarCelda(Graphics2D g, Rectangle celda, Color relleno, Color borde,
                                String texto, Color color, Font fuente) {
        g.setColor(relleno);
        g.fillRect(celda.x, celda.y, celda.width, celda.height);
        g.setColor(borde);
        g.drawRect(celda.x, celda.y, celda.width, celda.height);
        if (!texto.isEmpty()) {
            g.setFont(fuente);
            g.setColor(color);
            textoCentrado(g, texto, celda);
        }
    }

    /**
     * Escribe un texto centrado dentro de un rectángulo.
     *
     * @param g     lienzo, con la fuente ya puesta
     * @param texto texto a escribir
     * @param area  rectángulo donde centrarlo
     */
    protected void textoCentrado(Graphics2D g, String texto, Rectangle area) {
        FontMetrics metrica = g.getFontMetrics();
        int x = area.x + (area.width - metrica.stringWidth(texto)) / 2;
        int y = area.y + (area.height + metrica.getAscent() - metrica.getDescent()) / 2;
        g.drawString(texto, x, y);
    }

    /**
     * Dibuja un puntero: un triángulo con su nombre encima, apuntando hacia abajo.
     *
     * @param g      lienzo
     * @param centro coordenada horizontal a la que apunta
     * @param base   coordenada vertical de la punta
     * @param nombre texto del puntero, por ejemplo {@code "frente"}
     * @param color  color del puntero
     */
    protected void dibujarPuntero(Graphics2D g, int centro, int base, String nombre, Color color) {
        g.setColor(color);
        g.fillPolygon(
                new int[] {centro - 4, centro + 4, centro},
                new int[] {base - 6, base - 6, base},
                3);
        g.setFont(Colores.FUENTE_PEQUENA);
        int ancho = g.getFontMetrics().stringWidth(nombre);
        g.drawString(nombre, centro - ancho / 2, base - 8);
    }

    /**
     * Escribe un mensaje centrado en el área, para cuando una estructura está vacía.
     *
     * @param g       lienzo
     * @param area    zona disponible
     * @param mensaje texto a mostrar
     */
    protected void dibujarVacio(Graphics2D g, Rectangle area, String mensaje) {
        g.setFont(Colores.FUENTE_ETIQUETA);
        g.setColor(Colores.TEXTO_SUAVE);
        textoCentrado(g, mensaje, area);
    }
}
