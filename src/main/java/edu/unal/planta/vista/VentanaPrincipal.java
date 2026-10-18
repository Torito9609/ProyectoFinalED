package edu.unal.planta.vista;

import edu.unal.planta.modelo.ObservadorPlanta;
import edu.unal.planta.modelo.Planta;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Ventana principal: arma los paneles, los refresca cuando la planta cambia y traslada al
 * controlador lo que pide el operador.
 *
 * <p>Implementa {@link ObservadorPlanta}, así que la planta le avisa sin conocerla, y habla con el
 * controlador a través de {@link AccionesOperador}, así que tampoco lo conoce. La ventana está en
 * el medio sin depender de ninguno de los dos extremos.</p>
 *
 * <h2>Un solo punto de refresco</h2>
 * <p>La ventana guarda sus paneles en una lista de {@link PanelEstructura} y los refresca con una
 * sola llamada polimórfica. No sabe cuál panel dibuja la cola circular y cuál los carriles de
 * prioridad: cada uno sabe dibujarse a sí mismo. Agregar un panel nuevo es agregarlo a la lista.</p>
 *
 * <h2>Qué decide la ventana y qué no</h2>
 * <p>La selección de máquina y de sensor la resuelve aquí, porque no modifica la planta: solo
 * cambia lo que el operador está mirando. Todo lo que sí cambia el modelo —parar una máquina,
 * mover la carga, inyectar un fallo, deshacer, avanzar el reloj— se delega al controlador.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class VentanaPrincipal extends JFrame implements ObservadorPlanta {

    /** Todos los paneles, para refrescarlos con una sola llamada polimórfica. */
    private final List<PanelEstructura> paneles = new ArrayList<>();
    /** Muestra el tick actual y la semilla. */
    private final JLabel estado = new JLabel();
    /** Arranca o pausa el reloj; su texto refleja si la simulación está corriendo. */
    private final JButton botonEjecutar = new JButton("▶ Iniciar");
    /** Avanza un solo tick. */
    private final JButton botonPaso = new JButton("Paso");
    /** Vuelve al estado inicial con la misma semilla. */
    private final JButton botonReiniciar = new JButton("Reiniciar");

    /** Panel del arreglo dinámico de máquinas. */
    private final PanelInventario panelInventario = new PanelInventario();
    /** Panel con el estado y los sensores de la máquina seleccionada. */
    private final PanelDetalleMaquina panelDetalle = new PanelDetalleMaquina();
    /** Panel con las acciones que el operador puede ejecutar. */
    private final PanelControlesMaquina panelControles = new PanelControlesMaquina();
    /** Panel de la cola circular de lecturas. */
    private final PanelColaCircular panelCircular = new PanelColaCircular();
    /** Panel de la cola FIFO de eventos por clasificar. */
    private final PanelColaEventos panelEventos = new PanelColaEventos();
    /** Panel de la cola FIFO de técnicos disponibles. */
    private final PanelColaTecnicos panelTecnicos = new PanelColaTecnicos();
    /** Panel de los cuatro carriles de la cola con prioridad. */
    private final PanelColaPrioridad panelPrioridad = new PanelColaPrioridad();
    /** Panel de la pila de acciones deshacibles. */
    private final PanelPila panelPila = new PanelPila();
    /** Panel con el registro de lo que ocurre en cada tick. */
    private final PanelBitacora panelBitacora = new PanelBitacora();

    /** A quién se le piden las acciones del operador: el controlador, visto como interfaz. */
    private final AccionesOperador acciones;

    /** Qué máquina y qué sensor está mirando el operador. Es estado de la vista, no del modelo. */
    private SeleccionVista seleccion = SeleccionVista.INICIAL;
    /** Última planta recibida, para poder repintar cuando solo cambia la selección. */
    private Planta plantaActual;

    /**
     * Arma la ventana con todos sus paneles y la conecta con el controlador.
     *
     * @param acciones a quién pedirle las acciones del operador
     * @throws NullPointerException si las acciones son nulas
     */
    public VentanaPrincipal(AccionesOperador acciones) {
        super("Simulador de Monitoreo y Mantenimiento de Planta");
        this.acciones = Objects.requireNonNull(acciones, "La ventana necesita un controlador");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));

        paneles.add(panelInventario);
        paneles.add(panelDetalle);
        paneles.add(panelControles);
        paneles.add(panelCircular);
        paneles.add(panelEventos);
        paneles.add(panelTecnicos);
        paneles.add(panelPrioridad);
        paneles.add(panelPila);
        paneles.add(panelBitacora);

        conectarPaneles();

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
     * Enlaza cada panel con la acción que debe provocar.
     *
     * <p>Los paneles no reciben el controlador: avisan lo que pasó y la ventana decide a quién
     * trasladarlo. Por eso un panel se puede probar y dibujar sin que exista ningún controlador.</p>
     */
    private void conectarPaneles() {
        panelInventario.alSeleccionar(indice -> cambiarSeleccion(seleccion.conMaquina(indice)));
        panelControles.alSeleccionarSensor(indice -> cambiarSeleccion(seleccion.conSensor(indice)));
        panelControles.conectar(acciones);
        panelPila.alDeshacer(acciones::deshacer);

        botonEjecutar.addActionListener(evento -> {
            acciones.alternarEjecucion();
            sincronizarBotones();
        });
        botonPaso.addActionListener(evento -> {
            acciones.paso();
            sincronizarBotones();
        });
        botonReiniciar.addActionListener(evento -> {
            acciones.reiniciar();
            sincronizarBotones();
        });
    }

    /**
     * Construye la barra con los controles de la simulación y el estado del reloj.
     *
     * @return la barra superior
     */
    private JPanel barraSuperior() {
        JLabel titulo = new JLabel("Simulador de planta   ");
        titulo.setFont(Colores.FUENTE_TITULO);
        titulo.setForeground(Colores.TEXTO);

        botonEjecutar.setToolTipText("Arranca o pausa el reloj de la simulación");
        botonPaso.setToolTipText("Avanza un solo tick, para observar con calma qué cambió");
        botonReiniciar.setToolTipText("Vuelve al estado inicial con la misma semilla");

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.setBackground(Colores.PANEL);
        botones.add(botonEjecutar);
        botones.add(botonPaso);
        botones.add(botonReiniciar);

        estado.setFont(Colores.FUENTE_VALOR);
        estado.setForeground(Colores.TEXTO_SUAVE);
        estado.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Colores.PANEL);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colores.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        barra.add(titulo, BorderLayout.WEST);
        barra.add(botones, BorderLayout.CENTER);
        barra.add(estado, BorderLayout.EAST);
        return barra;
    }

    /**
     * Construye la columna del inventario, el detalle y los controles.
     *
     * @return la columna izquierda
     */
    private JPanel columnaIzquierda() {
        JPanel columna = new JPanel(new BorderLayout(0, 6));
        columna.setBackground(Colores.FONDO);
        columna.add(panelInventario, BorderLayout.NORTH);
        columna.add(panelDetalle, BorderLayout.CENTER);
        columna.add(panelControles, BorderLayout.SOUTH);
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
     * @param nueva selección nueva
     */
    public void cambiarSeleccion(SeleccionVista nueva) {
        this.seleccion = nueva;
        if (plantaActual != null) {
            plantaCambio(plantaActual);
        }
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
     * Ajusta el botón de arranque al estado real del reloj.
     */
    private void sincronizarBotones() {
        botonEjecutar.setText(acciones.estaCorriendo() ? "⏸ Pausar" : "▶ Iniciar");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Refresca todos los paneles con una sola llamada polimórfica. La ventana no sabe qué dibuja
     * cada uno.</p>
     */
    @Override
    public void plantaCambio(Planta planta) {
        this.plantaActual = planta;
        estado.setText("Tick: " + planta.tickActual() + "     Semilla: " + planta.semilla());
        sincronizarBotones();
        for (PanelEstructura panel : paneles) {
            panel.refrescar(planta, seleccion);
        }
    }
}
