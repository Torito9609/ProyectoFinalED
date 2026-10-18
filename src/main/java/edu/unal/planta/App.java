package edu.unal.planta;

import edu.unal.planta.modelo.ConfiguracionPlanta;
import edu.unal.planta.modelo.Planta;
import edu.unal.planta.vista.VentanaPrincipal;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>Su única responsabilidad es armar el modelo, la vista y el controlador, y entregarle el
 * control al hilo de eventos de Swing. No contiene lógica de simulación ni de dibujo: eso vive en
 * {@code edu.unal.planta.modelo} y en {@code edu.unal.planta.vista} respectivamente.</p>
 *
 * <p><strong>Estado provisional:</strong> todavía no existe el controlador, así que por ahora la
 * aplicación abre la ventana con una simulación ya avanzada y <em>congelada</em>, únicamente para
 * poder revisar cómo quedaron los paneles. El reloj y los botones se agregan en la etapa
 * siguiente, y entonces esta clase pasará a crear también el controlador.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class App {

    /** Nombre visible de la aplicación; se reutiliza en el título de la ventana. */
    public static final String NOMBRE = "Simulador de Monitoreo y Mantenimiento de Planta";

    /** Cuántos ticks se adelantan si no se indica otra cosa. */
    private static final int TICKS_POR_DEFECTO = 90;

    /** Constructor privado: esta clase solo expone el punto de entrada. */
    private App() {
    }

    /**
     * Arranca la aplicación.
     *
     * @param args opcionalmente, cuántos ticks adelantar antes de mostrar la ventana; sirve para
     *             revisar la interfaz en distintos momentos de la simulación
     */
    public static void main(String[] args) {
        int ticks = leerTicks(args);

        // Todo lo de Swing debe ocurrir en el hilo de eventos, incluida la construcción.
        SwingUtilities.invokeLater(() -> {
            Planta planta = new Planta(ConfiguracionPlanta.SEMILLA_POR_DEFECTO);
            for (int i = 0; i < ticks; i++) {
                planta.tick();
            }

            VentanaPrincipal ventana = new VentanaPrincipal();
            planta.agregarObservador(ventana);
            ventana.plantaCambio(planta);
            ventana.setVisible(true);
        });
    }

    /**
     * Lee cuántos ticks adelantar, con un valor por omisión si el argumento falta o no es válido.
     *
     * @param args argumentos de línea de comandos
     * @return número de ticks a adelantar
     */
    private static int leerTicks(String[] args) {
        if (args.length == 0) {
            return TICKS_POR_DEFECTO;
        }
        try {
            return Math.max(0, Integer.parseInt(args[0]));
        } catch (NumberFormatException sinNumero) {
            System.out.println("Argumento no numérico; se usan " + TICKS_POR_DEFECTO + " ticks.");
            return TICKS_POR_DEFECTO;
        }
    }
}
