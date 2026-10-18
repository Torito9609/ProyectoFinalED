package edu.unal.planta.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Muestra la bitácora de la simulación: una línea por cada cosa que pasó.
 *
 * <p>Es el único panel que no dibuja nada a mano. Un registro de texto necesita desplazamiento y
 * selección con el ratón, y eso {@link JTextArea} dentro de un {@link JScrollPane} ya lo resuelve:
 * programarlo con {@code Graphics2D} sería rehacer peor algo que Swing hace bien.</p>
 *
 * <p>Por eso usa el gancho {@code actualizar()} de la clase base en lugar de
 * {@code dibujarContenido}: cambia el texto del componente y deja que Swing lo pinte.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelBitacora extends PanelEstructura {

    /** Área donde se vuelca la bitácora. No es editable: el usuario solo lee y selecciona. */
    private final JTextArea texto = new JTextArea();

    /** Prepara el panel con su área de texto desplazable. */
    public PanelBitacora() {
        super("BITÁCORA");
        texto.setEditable(false);
        texto.setFont(Colores.FUENTE_ETIQUETA);
        texto.setForeground(Colores.TEXTO);
        texto.setBackground(Colores.PANEL);
        texto.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JScrollPane desplazable = new JScrollPane(texto);
        desplazable.setBorder(BorderFactory.createLineBorder(Colores.BORDE));
        desplazable.getViewport().setBackground(Colores.PANEL);

        add(desplazable, BorderLayout.CENTER);
        setPreferredSize(new Dimension(900, 150));
    }

    @Override
    protected String subtitulo() {
        return planta() == null ? "" : planta().bitacora().size() + " líneas";
    }

    @Override
    protected void actualizar() {
        if (planta() == null) {
            return;
        }
        texto.setText(String.join("\n", planta().bitacora()));
        // Deja a la vista en la última línea, que es lo que acaba de ocurrir.
        texto.setCaretPosition(texto.getDocument().getLength());
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        // El contenido lo pinta el JTextArea; aquí no hay nada que dibujar a mano.
    }
}
