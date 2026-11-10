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

    /** Cuántas lecturas recientes conserva cada máquina en su cola circular. */
    public static final int CAPACIDAD_LECTURAS = 12;

    /** Cuántas líneas conserva la bitácora antes de descartar las más viejas. */
    public static final int MAX_LINEAS_BITACORA = 200;

    /**
     * Duración de la reparación en ticks, indexada por {@link Prioridad#nivel()}.
     *
     * <p>Las órdenes más graves toman más tiempo. Estas duraciones, junto con las tasas base de
     * fallo de cada máquina, son las que deciden si la cola con prioridad forma filas visibles:
     * la cola se llena cuando las órdenes llegan más rápido de lo que los dos técnicos las
     * atienden.</p>
     */
    private static final int[] DURACION_REPARACION = {10, 8, 6, 4};

    /**
     * Pesos con que el generador elige la magnitud de una anomalía, indexados por el orden de
     * {@link Magnitud}: 80 % leve, 15 % moderada, 5 % severa.
     *
     * <p>Los pesos se eligieron para que la distribución de alarmas que produce la simulación
     * coincida con la recomendación de ISA-18.2: alrededor de 80 % de prioridad baja, 15 % media y
     * 5 % alta.</p>
     */
    private static final int[] PESO_MAGNITUD = {80, 15, 5};

    // ---------------------------------------------------------------------------------
    // Puntaje de fallos y recomendación (Entrega 2). Valores PROVISIONALES.
    // ---------------------------------------------------------------------------------

    /**
     * Puntaje de fallos a partir del cual se recomienda un overhaul. Con los pesos de
     * {@link Prioridad#peso()}, equivale por ejemplo a cuatro fallos P1 o doce fallos P3.
     */
    public static final int UMBRAL_OVERHAUL = 12;

    /**
     * Número de overhauls a partir del cual, si la máquina vuelve a superar el umbral, se
     * recomienda reemplazarla en vez de hacerle otro overhaul.
     */
    public static final int MAXIMO_OVERHAULS = 3;

    // ---------------------------------------------------------------------------------
    // Organización (Entrega 2). Valores PROVISIONALES.
    // ---------------------------------------------------------------------------------

    /** Nombres de las compañías con que arranca la organización; el id de cada una es su posición. */
    public static final List<String> NOMBRES_COMPANIAS =
            List.of("Planta Norte", "Planta Centro", "Planta Sur");

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
     * Crea las seis máquinas de la planta, en el orden en que aparecen en el inventario.
     *
     * <p>Son seis y no tres por una razón de la simulación: una máquina con anomalía activa no
     * desarrolla otra, así que el número de máquinas pone un techo a los fallos simultáneos. Con
     * tres máquinas y dos técnicos, la cola con prioridad nunca llegaba a tener más de una orden y
     * los paneles de colas se veían vacíos. Con seis, ese techo sube a cuatro.</p>
     *
     * <p>Ese techo es solo el límite superior. Lo que llena la cola de verdad es que las órdenes
     * lleguen más rápido de lo que los técnicos las atienden, y eso depende de la tasa base de
     * fallo y de la duración de las reparaciones, no del número de máquinas.</p>
     *
     * <p>Cada máquina recibe como índice su posición en la lista, así que el índice coincide con
     * su lugar en el inventario.</p>
     *
     * @param idCompania id de la compañía dueña de las máquinas
     * @return lista de máquinas recién creadas, todas operando
     */
    public static List<Maquina> crearMaquinas(int idCompania) {
        List<Maquina> maquinas = new ArrayList<>();
        for (TipoMaquina tipo : TipoMaquina.values()) {
            maquinas.add(crearMaquina(tipo, idCompania, maquinas.size(), tipo.prefijo() + "-01"));
        }
        return maquinas;
    }

    /**
     * Crea una máquina del catálogo, con los parámetros de su tipo.
     *
     * <p>Es el único lugar donde están los sensores, umbrales, intervalos, tasas de fallo y cargas
     * de cada tipo. Los usa tanto el inventario inicial como la creación de máquinas nuevas.</p>
     *
     * @param tipo       tipo de máquina
     * @param idCompania id de la compañía dueña
     * @param indice     posición que tendrá en el inventario
     * @param etiqueta   identificador visible
     * @return la máquina nueva, operando
     */
    public static Maquina crearMaquina(TipoMaquina tipo, int idCompania, int indice, String etiqueta) {
        return switch (tipo) {
            case PRENSA -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura aceite", "°C", 55.0, 2.0, 70.0, 80.0, 90.0),
                            new Sensor("Presión", "bar", 180.0, 4.0, 210.0, 230.0, 250.0),
                            new Sensor("Vibración", "mm/s", 2.5, 0.4, 4.5, 7.1, 11.0)),
                    60,     // intervalo preventivo en ticks
                    0.06,   // tasa base de fallo: es la máquina más crítica, falla más
                    80,     // carga inicial
                    CAPACIDAD_LECTURAS);
            case TORNO -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura husillo", "°C", 45.0, 2.0, 60.0, 70.0, 85.0),
                            new Sensor("Corriente motor", "A", 12.0, 0.8, 16.0, 19.0, 22.0),
                            new Sensor("Vibración", "mm/s", 1.8, 0.3, 2.8, 4.5, 7.1)),
                    80,
                    0.04,
                    70,
                    CAPACIDAD_LECTURAS);
            case ROBOT -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura motor", "°C", 40.0, 2.0, 55.0, 65.0, 80.0),
                            new Sensor("Corriente", "A", 8.0, 0.6, 11.0, 13.0, 16.0),
                            new Sensor("Vibración", "mm/s", 1.4, 0.25, 2.8, 4.5, 7.1)),
                    100,
                    0.03,
                    60,
                    CAPACIDAD_LECTURAS);
            case COMPRESOR -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura descarga", "°C", 85.0, 3.0, 100.0, 110.0, 125.0),
                            new Sensor("Presión", "bar", 7.5, 0.2, 9.0, 9.8, 11.0),
                            new Sensor("Vibración", "mm/s", 2.0, 0.3, 4.5, 7.1, 11.0)),
                    70,
                    0.05,
                    75,
                    CAPACIDAD_LECTURAS);
            case HORNO -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura cámara", "°C", 850.0, 10.0, 900.0, 940.0, 980.0),
                            new Sensor("Temperatura carcasa", "°C", 60.0, 3.0, 80.0, 95.0, 110.0),
                            new Sensor("Consumo de gas", "m³/h", 42.0, 2.0, 52.0, 58.0, 68.0)),
                    90,
                    0.035,
                    65,
                    CAPACIDAD_LECTURAS);
            case BOMBA -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura rodamiento", "°C", 50.0, 2.0, 65.0, 75.0, 90.0),
                            new Sensor("Presión descarga", "bar", 6.0, 0.25, 8.0, 9.0, 10.5),
                            new Sensor("Vibración", "mm/s", 2.2, 0.35, 4.5, 7.1, 11.0)),
                    55,
                    0.055,
                    85,
                    CAPACIDAD_LECTURAS);
        };
    }

    /**
     * Crea las seis máquinas para la compañía 0. Es la planta única de la Entrega 1.
     *
     * @return lista de máquinas recién creadas, todas operando
     */
    public static List<Maquina> crearMaquinas() {
        return crearMaquinas(0);
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
