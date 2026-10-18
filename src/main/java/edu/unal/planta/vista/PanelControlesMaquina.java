package edu.unal.planta.vista;

import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Magnitud;
import edu.unal.planta.modelo.Maquina;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Controles con que el operador actúa sobre la máquina seleccionada.
 *
 * <p>Separado del panel de detalle a propósito: aquel solo muestra información y se dibuja a mano,
 * mientras que este necesita componentes de Swing de verdad —listas desplegables, un deslizador,
 * botones— que ya resuelven la interacción con el teclado y el ratón.</p>
 *
 * <p>El panel no conoce la planta ni el controlador: cuando el operador hace algo, se lo pide a
 * {@link AccionesOperador}. Lo único que lee del modelo es el estado de la máquina, para saber qué
 * ofrecer: no tiene sentido habilitar "Parar" en una máquina que un técnico está reparando.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelControlesMaquina extends PanelEstructura {

    private final JComboBox<String> listaSensores = new JComboBox<>();
    private final JSlider deslizadorCarga = new JSlider(0, 100, 50);
    private final JLabel valorCarga = new JLabel("50%");
    private final JButton botonPararArrancar = new JButton("Parar");
    private final JComboBox<String> listaMagnitudes = new JComboBox<>();
    private final JButton botonInyectar = new JButton("Inyectar fallo");

    private AccionesOperador acciones;
    private IntConsumer alSeleccionarSensor = indice -> { };

    /** Evita que al sincronizar los controles con el modelo se disparen acciones del operador. */
    private boolean sincronizando;

    /** Recuerda de qué máquina se llenó la lista de sensores, para no rehacerla en cada tick. */
    private int maquinaDeLaLista = -1;

    /** Arma los controles. */
    public PanelControlesMaquina() {
        super("ACCIONES DEL OPERADOR");
        setPreferredSize(new Dimension(250, 150));

        for (Magnitud magnitud : Magnitud.values()) {
            listaMagnitudes.addItem(magnitud.descripcion());
        }
        listaMagnitudes.setSelectedIndex(Magnitud.SEVERA.ordinal());

        JPanel filas = new JPanel();
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        filas.setBackground(Colores.PANEL);
        filas.add(fila("Sensor", listaSensores));
        filas.add(filaCarga());
        filas.add(fila("Magnitud", listaMagnitudes));
        filas.add(filaBotones());

        add(filas, BorderLayout.CENTER);
        conectarControles();
    }

    /**
     * Conecta el panel con quien ejecuta las acciones del operador.
     *
     * @param acciones destinatario de las peticiones del usuario
     */
    public void conectar(AccionesOperador acciones) {
        this.acciones = acciones;
    }

    /**
     * Registra a quién avisarle cuando el operador elija otro sensor.
     *
     * @param accion qué hacer con el índice del sensor elegido
     */
    public void alSeleccionarSensor(IntConsumer accion) {
        this.alSeleccionarSensor = accion;
    }

    /** Enlaza cada control con la acción que le corresponde. */
    private void conectarControles() {
        listaSensores.addActionListener(evento -> {
            if (!sincronizando && listaSensores.getSelectedIndex() >= 0) {
                alSeleccionarSensor.accept(listaSensores.getSelectedIndex());
            }
        });

        deslizadorCarga.addChangeListener(evento -> {
            valorCarga.setText(deslizadorCarga.getValue() + "%");
            boolean terminoDeArrastrar = !deslizadorCarga.getValueIsAdjusting();
            if (!sincronizando && terminoDeArrastrar && acciones != null) {
                acciones.cambiarCarga(seleccion().indiceMaquina(), deslizadorCarga.getValue());
            }
        });

        botonPararArrancar.addActionListener(evento -> {
            if (acciones != null) {
                acciones.pararOArrancar(seleccion().indiceMaquina());
            }
        });

        botonInyectar.addActionListener(evento -> {
            if (acciones != null) {
                acciones.inyectarFallo(
                        seleccion().indiceMaquina(),
                        Math.max(0, listaSensores.getSelectedIndex()),
                        Magnitud.values()[listaMagnitudes.getSelectedIndex()]);
            }
        });
    }

    @Override
    protected void actualizar() {
        if (planta() == null) {
            return;
        }
        sincronizando = true;
        try {
            Maquina maquina = planta().maquina(seleccion().indiceMaquina());
            sincronizarSensores(maquina);
            sincronizarCarga(maquina);
            sincronizarBotonDeEstado(maquina);
        } finally {
            sincronizando = false;
        }
    }

    /**
     * Rellena la lista de sensores, solo si cambió la máquina seleccionada.
     *
     * @param maquina máquina seleccionada
     */
    private void sincronizarSensores(Maquina maquina) {
        if (maquinaDeLaLista != seleccion().indiceMaquina()) {
            listaSensores.removeAllItems();
            maquina.sensores().forEach(sensor -> listaSensores.addItem(sensor.nombre()));
            maquinaDeLaLista = seleccion().indiceMaquina();
        }
        int sensor = Math.min(seleccion().indiceSensor(), maquina.sensores().size() - 1);
        if (listaSensores.getSelectedIndex() != sensor) {
            listaSensores.setSelectedIndex(sensor);
        }
    }

    /**
     * Pone el deslizador en la carga real de la máquina, salvo que el operador lo esté arrastrando.
     *
     * @param maquina máquina seleccionada
     */
    private void sincronizarCarga(Maquina maquina) {
        if (!deslizadorCarga.getValueIsAdjusting()) {
            deslizadorCarga.setValue(maquina.carga());
            valorCarga.setText(maquina.carga() + "%");
        }
    }

    /**
     * Ajusta el botón de parar o arrancar al estado de la máquina.
     *
     * <p>Solo tiene sentido con una máquina operando o parada a mano. Si la está atendiendo un
     * técnico, o la detuvo una alarma crítica, el botón se deshabilita en lugar de dejar al
     * operador pulsar algo que no va a ocurrir.</p>
     *
     * @param maquina máquina seleccionada
     */
    private void sincronizarBotonDeEstado(Maquina maquina) {
        boolean parada = maquina.estado() == EstadoMaquina.PARADA_MANUAL;
        boolean operando = maquina.estado() == EstadoMaquina.OPERANDO;
        botonPararArrancar.setText(parada ? "Arrancar" : "Parar");
        botonPararArrancar.setEnabled(parada || operando);
        botonPararArrancar.setToolTipText(parada || operando
                ? null
                : "No se puede: la máquina está " + maquina.estado().descripcion().toLowerCase());
    }

    /**
     * Arma una fila con su etiqueta a la izquierda y el control a la derecha.
     *
     * @param etiqueta texto de la izquierda
     * @param control  componente de la derecha
     * @return la fila armada
     */
    private JPanel fila(String etiqueta, JComboBox<String> control) {
        JLabel titulo = new JLabel(etiqueta);
        titulo.setFont(Colores.FUENTE_ETIQUETA);
        titulo.setForeground(Colores.TEXTO_SUAVE);
        titulo.setPreferredSize(new Dimension(58, 22));
        control.setFont(Colores.FUENTE_ETIQUETA);

        JPanel fila = new JPanel(new BorderLayout(4, 0));
        fila.setBackground(Colores.PANEL);
        fila.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        fila.add(titulo, BorderLayout.WEST);
        fila.add(control, BorderLayout.CENTER);
        return fila;
    }

    /**
     * Arma la fila del deslizador de carga.
     *
     * @return la fila armada
     */
    private JPanel filaCarga() {
        JLabel titulo = new JLabel("Carga");
        titulo.setFont(Colores.FUENTE_ETIQUETA);
        titulo.setForeground(Colores.TEXTO_SUAVE);
        titulo.setPreferredSize(new Dimension(58, 22));

        valorCarga.setFont(Colores.FUENTE_VALOR);
        valorCarga.setForeground(Colores.TEXTO);
        valorCarga.setPreferredSize(new Dimension(36, 22));

        deslizadorCarga.setBackground(Colores.PANEL);
        deslizadorCarga.setToolTipText("Más carga, más probabilidad de fallo");

        JPanel fila = new JPanel(new BorderLayout(4, 0));
        fila.setBackground(Colores.PANEL);
        fila.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        fila.add(titulo, BorderLayout.WEST);
        fila.add(deslizadorCarga, BorderLayout.CENTER);
        fila.add(valorCarga, BorderLayout.EAST);
        return fila;
    }

    /**
     * Arma la fila de los dos botones.
     *
     * @return la fila armada
     */
    private JPanel filaBotones() {
        botonPararArrancar.setFont(Colores.FUENTE_ETIQUETA);
        botonInyectar.setFont(Colores.FUENTE_ETIQUETA);
        botonInyectar.setToolTipText("Provoca una anomalía a mano, para forzar un caso en la demostración");

        JPanel fila = new JPanel(new GridLayout(1, 2, 4, 0));
        fila.setBackground(Colores.PANEL);
        fila.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        fila.add(botonPararArrancar);
        fila.add(botonInyectar);
        return fila;
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        // Todo el contenido son componentes de Swing; aquí no hay nada que dibujar a mano.
    }
}
