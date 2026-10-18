package edu.unal.planta;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>Su única responsabilidad es armar el modelo, la vista y el controlador, y
 * entregarle el control al hilo de eventos de Swing. No contiene lógica de
 * simulación ni de dibujo: eso vive en {@code edu.unal.planta.modelo} y en
 * {@code edu.unal.planta.vista} respectivamente.</p>
 *
 * <p>En esta etapa del proyecto solo existe el esqueleto de paquetes, así que
 * la clase se limita a informar el estado de la construcción. El armado real
 * del patrón MVC se agrega cuando la vista y el controlador existan.</p>
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
     * @param args argumentos de línea de comandos; no se usan
     */
    public static void main(String[] args) {
        System.out.println(NOMBRE);
        System.out.println("Universidad Nacional de Colombia - Estructuras de Datos");
        System.out.println();
        System.out.println("Esqueleto del proyecto listo. La interfaz se construye en una etapa posterior.");
    }
}
