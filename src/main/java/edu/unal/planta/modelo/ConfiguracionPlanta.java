package edu.unal.planta.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Todos los parámetros de la simulación, en un solo archivo.
 *
 * <p>Ninguna otra clase del proyecto debe tener números sueltos. La idea es que para ajustar el
 * comportamiento de la demostración —que falle más seguido, que las reparaciones duren menos, que
 * los umbrales queden más cerca— se edite únicamente este archivo, sin tocar la lógica.</p>
 *
 * <h2>VALORES PROVISIONALES</h2>
 * <p>Los valores de máquinas, sensores y umbrales son provisionales y los define Cristian. Al
 * cambiarlos hay que respetar una condición, que el constructor de {@link Sensor} verifica:
 * <strong>nominal + ruido debe quedar claramente por debajo del umbral de advertencia</strong>, o
 * aparecerán alarmas falsas por puro ruido, sin que exista ninguna anomalía.</p>
 *
 * <p>Los umbrales de vibración usan las zonas de la norma <strong>ISO 10816</strong> para máquinas
 * industriales (4,5 / 7,1 / 11,0 mm/s), así que ese dato sí es real y se puede defender.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class ConfiguracionPlanta {

    // ---------------------------------------------------------------------------------
    // Simulación
    // ---------------------------------------------------------------------------------

    /** Semilla por omisión del generador aleatorio. La misma semilla repite la misma simulación. */
    public static final long SEMILLA_POR_DEFECTO = 7L;

    /** Milisegundos entre ticks cuando la simulación corre sola. */
    public static final int MILIS_POR_TICK = 800;

    /** Cuántas lecturas recientes conserva cada máquina en su cola circular. */
    public static final int CAPACIDAD_LECTURAS = 12;

    /** Cuántas líneas conserva la bitácora antes de descartar las más viejas. */
    public static final int MAX_LINEAS_BITACORA = 200;

    /**
     * Duración de la reparación en ticks, indexada por {@link Prioridad#nivel()}.
     *
     * <p>Las órdenes más graves toman más tiempo, lo que hace que se formen filas visibles en la
     * cola con prioridad.</p>
     */
    private static final int[] DURACION_REPARACION = {8, 6, 4, 3};

    /**
     * Pesos con que el generador elige la magnitud de una anomalía, indexados por el orden de
     * {@link Magnitud}: 80 % leve, 15 % moderada, 5 % severa.
     *
     * <p>Los pesos se eligieron para que la distribución de alarmas que produce la simulación
     * coincida con la recomendación de ISA-18.2: alrededor de 80 % de prioridad baja, 15 % media y
     * 5 % alta.</p>
     */
    private static final int[] PESO_MAGNITUD = {80, 15, 5};

    /** Nombres de los técnicos. Son menos que las máquinas, para que se formen filas. */
    private static final List<String> NOMBRES_TECNICOS = List.of("Ana", "Luis");

    /** Clase de utilidades: no se instancia. */
    private ConfiguracionPlanta() {
    }

    /**
     * Devuelve cuántos ticks tarda atender una orden de esa prioridad.
     *
     * @param prioridad prioridad de la orden
     * @return duración estimada en ticks
     */
    public static int duracionReparacion(Prioridad prioridad) {
        return DURACION_REPARACION[prioridad.nivel()];
    }

    /**
     * Devuelve el peso relativo de una magnitud en el sorteo del generador de fallos.
     *
     * @param magnitud magnitud consultada
     * @return peso, en las mismas unidades que los demás
     */
    public static int pesoMagnitud(Magnitud magnitud) {
        return PESO_MAGNITUD[magnitud.ordinal()];
    }

    /**
     * Devuelve la suma de los pesos de todas las magnitudes, para el sorteo.
     *
     * @return suma de pesos
     */
    public static int pesoTotalMagnitudes() {
        int total = 0;
        for (int peso : PESO_MAGNITUD) {
            total += peso;
        }
        return total;
    }

    // ---------------------------------------------------------------------------------
    // Inventario inicial
    // ---------------------------------------------------------------------------------

    /**
     * Crea las tres máquinas de la planta, en el orden en que aparecen en el inventario.
     *
     * @return lista de máquinas recién creadas, todas operando
     */
    public static List<Maquina> crearMaquinas() {
        List<Maquina> maquinas = new ArrayList<>();

        maquinas.add(new Maquina(
                "PRENSA-01",
                "Prensa hidráulica",
                List.of(
                        new Sensor("Temperatura aceite", "°C", 55.0, 2.0, 70.0, 80.0, 90.0),
                        new Sensor("Presión", "bar", 180.0, 4.0, 210.0, 230.0, 250.0),
                        new Sensor("Vibración", "mm/s", 2.5, 0.4, 4.5, 7.1, 11.0)),
                60,     // intervalo preventivo en ticks
                0.06,   // tasa base de fallo: es la máquina más crítica, falla más
                80,     // carga inicial
                CAPACIDAD_LECTURAS));

        maquinas.add(new Maquina(
                "TORNO-01",
                "Torno CNC",
                List.of(
                        new Sensor("Temperatura husillo", "°C", 45.0, 2.0, 60.0, 70.0, 85.0),
                        new Sensor("Corriente motor", "A", 12.0, 0.8, 16.0, 19.0, 22.0),
                        new Sensor("Vibración", "mm/s", 1.8, 0.3, 2.8, 4.5, 7.1)),
                80,
                0.04,
                70,
                CAPACIDAD_LECTURAS));

        maquinas.add(new Maquina(
                "ROBOT-01",
                "Brazo robótico de soldadura",
                List.of(
                        new Sensor("Temperatura motor", "°C", 40.0, 2.0, 55.0, 65.0, 80.0),
                        new Sensor("Corriente", "A", 8.0, 0.6, 11.0, 13.0, 16.0),
                        new Sensor("Vibración", "mm/s", 1.4, 0.25, 2.8, 4.5, 7.1)),
                100,
                0.03,
                60,
                CAPACIDAD_LECTURAS));

        return maquinas;
    }

    /**
     * Crea los técnicos de mantenimiento, todos libres.
     *
     * @return lista de técnicos recién creados
     */
    public static List<Tecnico> crearTecnicos() {
        List<Tecnico> tecnicos = new ArrayList<>();
        for (String nombre : NOMBRES_TECNICOS) {
            tecnicos.add(new Tecnico(nombre));
        }
        return tecnicos;
    }
}
