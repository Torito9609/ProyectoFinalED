package edu.unal.planta;

import edu.unal.planta.controlador.ControladorPlanta;
import edu.unal.planta.modelo.ConfiguracionPlanta;
import edu.unal.planta.modelo.Planta;
import edu.unal.planta.vista.VentanaPrincipal;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>Su única responsabilidad es armar el patrón Modelo-Vista-Controlador y entregarle el control
 * al hilo de eventos de Swing. No contiene lógica de simulación ni de dibujo.</p>
 *
 * <p>Las tres piezas se enlazan en un solo sentido, y aquí se ve por qué la arquitectura se
 * sostiene:</p>
 * <ol>
 *   <li>Se crea la {@link Planta}, que no conoce a nadie.</li>
 *   <li>Se crea el {@link ControladorPlanta} sobre ella, que conoce el modelo.</li>
 *   <li>Se crea la {@link VentanaPrincipal} sobre el controlador, al que ve solo como la interfaz
 *       {@code AccionesOperador}.</li>
 *   <li>La ventana se registra como observadora de la planta.</li>
 * </ol>
 *
 * <p>Esta clase es la única del proyecto que conoce los tres paquetes a la vez. Es el precio
 * normal de armar las piezas en algún lado, y por eso no hace nada más.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class App {

    /** Nombre visible de la aplicación; se reutiliza en el título de la ventana. */
    public static final String NOMBRE = "Simulador de Monitoreo y Mantenimiento de Planta";

    /** Constructor privado: esta clase solo expone el punto de entrada. */
    private App() {
    }

    /**
     * Arranca la aplicación.
     *
     * @param args opcionalmente, la semilla del generador aleatorio; con la misma semilla la
     *             simulación se repite igual, que es lo que permite ensayar la demostración
     */
    public static void main(String[] args) {
        long semilla = leerSemilla(args);

        // Todo lo de Swing debe ocurrir en el hilo de eventos, incluida la construcción.
        SwingUtilities.invokeLater(() -> {
            Planta planta = new Planta(semilla);
            ControladorPlanta controlador = new ControladorPlanta(planta);
            VentanaPrincipal ventana = new VentanaPrincipal(controlador);

            planta.agregarObservador(ventana);
            ventana.plantaCambio(planta);
            ventana.setVisible(true);
        });
    }

    /**
     * Lee la semilla de los argumentos, con un valor por omisión si falta o no es válida.
     *
     * @param args argumentos de línea de comandos
     * @return semilla a usar
     */
    private static long leerSemilla(String[] args) {
        if (args.length == 0) {
            return ConfiguracionPlanta.SEMILLA_POR_DEFECTO;
        }
        try {
            return Long.parseLong(args[0]);
        } catch (NumberFormatException sinNumero) {
            System.out.println("Semilla no numérica; se usa "
                    + ConfiguracionPlanta.SEMILLA_POR_DEFECTO + ".");
            return ConfiguracionPlanta.SEMILLA_POR_DEFECTO;
        }
    }
}
