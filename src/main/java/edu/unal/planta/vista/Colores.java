package edu.unal.planta.vista;

import edu.unal.planta.modelo.Prioridad;
import edu.unal.planta.modelo.Sensor;
import java.awt.Color;
import java.awt.Font;

/**
 * Paleta y tipografías de la interfaz, en un solo lugar.
 *
 * <p>Es además el punto donde los colores del modelo se vuelven colores de Swing. {@code Prioridad}
 * y {@code EstadoMaquina} guardan el suyo como un entero {@code 0xRRGGBB}, porque el modelo no
 * puede importar {@code java.awt}: el modelo decide <em>qué</em> color corresponde y esta clase sabe
 * cómo pintarlo.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Colores {

    /** Fondo de la ventana. */
    public static final Color FONDO = new Color(0xECEFF1);

    /** Fondo de cada panel. */
    public static final Color PANEL = Color.WHITE;

    /** Borde de paneles y celdas. */
    public static final Color BORDE = new Color(0xB0BEC5);

    /** Texto principal. */
    public static final Color TEXTO = new Color(0x212121);

    /** Texto secundario: índices, notas, unidades. */
    public static final Color TEXTO_SUAVE = new Color(0x78909C);

    /** Relleno de una celda vacía. */
    public static final Color CELDA_VACIA = new Color(0xF0F3F4);

    /** Color con que se marca lo seleccionado o lo próximo en salir. */
    public static final Color RESALTADO = new Color(0x1565C0);

    /** Valor de sensor dentro de lo normal. */
    public static final Color ZONA_NORMAL = new Color(0x2E7D32);

    /** Título de panel. */
    public static final Font FUENTE_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    /** Etiquetas normales. */
    public static final Font FUENTE_ETIQUETA = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    /** Etiquetas destacadas. */
    public static final Font FUENTE_FUERTE = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    /** Números, de ancho fijo para que las columnas queden alineadas. */
    public static final Font FUENTE_VALOR = new Font(Font.MONOSPACED, Font.BOLD, 11);

    /** Índices de celda y notas al pie. */
    public static final Font FUENTE_PEQUENA = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

    /** Clase de utilidades: no se instancia. */
    private Colores() {
    }

    /**
     * Convierte un color del modelo, empacado como {@code 0xRRGGBB}, en un color de Swing.
     *
     * @param rgb color empacado en un entero
     * @return el color equivalente
     */
    public static Color de(int rgb) {
        return new Color(rgb);
    }

    /**
     * Devuelve el color que corresponde a un valor leído, según la zona de umbrales en que caiga.
     *
     * <p>Usa los mismos colores de las prioridades, para que el operador asocie de inmediato el
     * color de una lectura con la gravedad de la alarma que produciría.</p>
     *
     * @param sensor sensor que produjo la lectura
     * @param valor  valor leído
     * @return verde si es normal, y el color de P3, P2 o P1 según la zona
     */
    public static Color zonaDe(Sensor sensor, double valor) {
        if (valor >= sensor.umbralCritico()) {
            return de(Prioridad.P1.rgb());
        }
        if (valor >= sensor.umbralAlto()) {
            return de(Prioridad.P2.rgb());
        }
        if (valor >= sensor.umbralAdvertencia()) {
            return de(Prioridad.P3.rgb());
        }
        return ZONA_NORMAL;
    }

    /**
     * Mezcla un color con blanco, para usarlo como relleno detrás de texto oscuro.
     *
     * @param color  color base
     * @param cuanto cuánto blanco se mezcla, entre 0 y 1
     * @return el color aclarado
     */
    public static Color aclarar(Color color, double cuanto) {
        return new Color(
                (int) (color.getRed() + (255 - color.getRed()) * cuanto),
                (int) (color.getGreen() + (255 - color.getGreen()) * cuanto),
                (int) (color.getBlue() + (255 - color.getBlue()) * cuanto));
    }
}
