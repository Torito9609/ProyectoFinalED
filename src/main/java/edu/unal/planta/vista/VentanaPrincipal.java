package edu.unal.planta.vista;

import edu.unal.planta.modelo.ObservadorPlanta;
import edu.unal.planta.modelo.Planta;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Ventana principal: arma los paneles y los refresca cuando la planta cambia.
 *
 * <p>Implementa {@link ObservadorPlanta}, así que la planta le avisa sin conocerla. Esa es la pieza
 * que mantiene la separación del modelo y la vista: el modelo no importa nada de Swing y, de
 * hecho, la simulación corre igual sin que exista esta clase.</p>
 *
 * <h2>Un solo punto de refresco</h2>
 * <p>La ventana guarda sus paneles en una lista de {@link PanelEstructura} y los refresca con una
 * sola llamada polimórfica. No sabe cuál panel dibuja la cola circular y cuál los carriles de
 * prioridad: cada uno sabe dibujarse a sí mismo. Agregar un panel nuevo es agregarlo a la lista.</p>
 *
 * <p>En esta etapa la ventana solo muestra. Los botones y la selección con el ratón llegan con el
 * controlador, porque toda acción del usuario debe pasar por él.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class VentanaPrincipal extends JFrame implements ObservadorPlanta {

    private final List<PanelEstructura> paneles = new ArrayList<>();
    private final JLabel estado = new JLabel();

    private final PanelInventario panelInventario = new PanelInventario();
    private final PanelDetalleMaquina panelDetalle = new PanelDetalleMaquina();
    private final PanelColaCircular panelCircular = new PanelColaCircular();
    private final PanelColaEventos panelEventos = new PanelColaEventos();
    private final PanelColaTecnicos panelTecnicos = new PanelColaTecnicos();
    private final PanelColaPrioridad panelPrioridad = new PanelColaPrioridad();
    private final PanelPila panelPila = new PanelPila();
    private final PanelBitacora panelBitacora = new PanelBitacora();

    private SeleccionVista seleccion = SeleccionVista.INICIAL;

    /**
     * Arma la ventana con todos sus paneles.
     */
    public VentanaPrincipal() {
        super("Simulador de Monitoreo y Mantenimiento de Planta");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));

        paneles.add(panelInventario);
        paneles.add(panelDetalle);
        paneles.add(panelCircular);
        paneles.add(panelEventos);
        paneles.add(panelTecnicos);
        paneles.add(panelPrioridad);
        paneles.add(panelPila);
        paneles.add(panelBitacora);

        // Los paneles de estructuras conservan su alto natural, y el espacio sobrante de la
        // ventana se lo lleva la bitácora, que es la única que gana algo con ser más alta.
        JPanel tablero = new JPanel(new BorderLayout(6, 6));
        tablero.setBackground(Colores.FONDO);
        tablero.add(columnaIzquierda(), BorderLayout.WEST);
        tablero.add(zonaCentral(), BorderLayout.CENTER);
        tablero.add(panelPila, BorderLayout.EAST);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 6));
        cuerpo.setBackground(Colores.FONDO);
        cuerpo.add(tablero, BorderLayout.NORTH);
        cuerpo.add(panelBitacora, BorderLayout.CENTER);

        JPanel raiz = new JPanel(new BorderLayout(6, 6));
        raiz.setBackground(Colores.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        raiz.add(barraSuperior(), BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);

        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Construye la barra donde se muestran el tick y la semilla.
     *
     * <p>Los botones de control se agregan cuando exista el controlador.</p>
     *
     * @return la barra superior
     */
    private JPanel barraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Colores.PANEL);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colores.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JLabel titulo = new JLabel("Simulador de planta");
        titulo.setFont(Colores.FUENTE_TITULO);
        titulo.setForeground(Colores.TEXTO);

        estado.setFont(Colores.FUENTE_VALOR);
        estado.setForeground(Colores.TEXTO_SUAVE);
        estado.setHorizontalAlignment(SwingConstants.RIGHT);

        barra.add(titulo, BorderLayout.WEST);
        barra.add(estado, BorderLayout.EAST);
        return barra;
    }

    /**
     * Construye la columna del inventario y el detalle de la máquina.
     *
     * @return la columna izquierda
     */
    private JPanel columnaIzquierda() {
        JPanel columna = new JPanel(new BorderLayout(0, 6));
        columna.setBackground(Colores.FONDO);
        columna.add(panelInventario, BorderLayout.NORTH);
        columna.add(panelDetalle, BorderLayout.CENTER);
        return columna;
    }

    /**
     * Construye el centro: la cola circular arriba, las dos colas FIFO en el medio y la cola con
     * prioridad abajo.
     *
     * @return la zona central
     */
    private JPanel zonaCentral() {
        JPanel colas = new JPanel(new GridLayout(1, 2, 6, 0));
        colas.setBackground(Colores.FONDO);
        colas.add(panelEventos);
        colas.add(panelTecnicos);

        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        arriba.setBackground(Colores.FONDO);
        arriba.add(panelCircular, BorderLayout.NORTH);
        arriba.add(colas, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setBackground(Colores.FONDO);
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(panelPrioridad, BorderLayout.CENTER);
        return centro;
    }

    /**
     * Cambia qué máquina y qué sensor se están mirando, y vuelve a dibujar.
     *
     * @param nueva  selección nueva
     * @param planta planta que hay que volver a mostrar
     */
    public void cambiarSeleccion(SeleccionVista nueva, Planta planta) {
        this.seleccion = nueva;
        plantaCambio(planta);
    }

    /**
     * Devuelve qué está mirando el operador.
     *
     * @return la selección actual
     */
    public SeleccionVista seleccion() {
        return seleccion;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Refresca todos los paneles con una sola llamada polimórfica. La ventana no sabe qué dibuja
     * cada uno.</p>
     */
    @Override
    public void plantaCambio(Planta planta) {
        estado.setText("Tick: " + planta.tickActual() + "     Semilla: " + planta.semilla());
        for (PanelEstructura panel : paneles) {
            panel.refrescar(planta, seleccion);
        }
    }
}
