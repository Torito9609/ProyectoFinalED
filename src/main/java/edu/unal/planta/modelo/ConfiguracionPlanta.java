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
     * {@link Prioridad#peso()}, equivale por ejemplo a diez fallos P1 o treinta fallos P3.
     *
     * <p>Subió de 12 a 30 con las partidas de 1000 ticks: con 12, la mitad de las máquinas lo
     * cruzaba antes del tick 250 y todas al final. Con 30, las primeras lo cruzan a mitad de
     * partida, cuando todavía vale la pena pagar un overhaul.</p>
     */
    public static final int UMBRAL_OVERHAUL = 30;

    /**
     * Número de overhauls a partir del cual, si la máquina vuelve a superar el umbral, se
     * recomienda reemplazarla en vez de hacerle otro overhaul.
     *
     * <p>Bajó de 3 a 1: con 3, la recomendación de reemplazo no podía aparecer en una partida de
     * 1000 ticks.</p>
     */
    public static final int MAXIMO_OVERHAULS = 1;

    /**
     * Techo del factor de historial de {@link Maquina#probabilidadFallo()}: una máquina con el
     * puntaje en el umbral, o más, falla el doble.
     */
    public static final double MAX_FACTOR_HISTORIAL = 2.0;

    /** Cuánto sube la tasa base de fallo por cada overhaul recibido: cada overhaul rinde menos. */
    public static final double INCREMENTO_TASA_POR_OVERHAUL = 0.25;

    /**
     * Lo que cuesta un overhaul, cobrado al despachar el camión del fabricante. Con el puntaje en
     * el umbral, la máquina pierde unos 550 por tick en fallos de más: el overhaul se recupera en
     * unos 250 ticks, así que conviene a mitad de partida y no al final.
     */
    public static final long COSTO_OVERHAUL = 120_000L;

    /**
     * Lo que cuesta reemplazar una máquina, cobrado de una vez: dos veces y media un overhaul. La
     * máquina nueva arranca sin desgaste ni overhauls, así que conviene cuando el overhaul ya no
     * rinde.
     */
    public static final long COSTO_REEMPLAZO = 300_000L;

    // ---------------------------------------------------------------------------------
    // Economía y partida (Entrega 2). Valores PROVISIONALES, recalibrados en el commit 14d con 4
    // técnicos y las tasas × 1,5: en automático una compañía termina con unos 215 mil de caja, cerca
    // de 350 fallos y las máquinas un 15 % del tiempo en alarma (5 semillas × 3 compañías).
    // ---------------------------------------------------------------------------------

    /** Dinero con que arranca cada compañía: unos 100 ticks de ingreso de la planta. */
    public static final long PRESUPUESTO_INICIAL = 300_000L;

    /** Lo que produce una máquina en un tick a carga 100; a otra carga, en proporción. */
    public static final long INGRESO_POR_TICK = 1_000L;

    /**
     * Porcentaje de su producción que da una máquina en alarma (commit 14c). Con el fallo en curso
     * trabaja degradada: sin este castigo, un fallo casi solo costaba mano de obra, y en una partida
     * automática las máquinas pasan cerca del 40 % del tiempo esperando técnico en alarma.
     */
    public static final int PRODUCCION_EN_ALARMA = 50;

    /**
     * Lo que cuesta un técnico por cada tick que pasa trabajando. Era 1.500 con 2 técnicos; con 4
     * (commit 14d) ese valor llevaba a la quiebra a casi todas las compañías.
     */
    public static final long MANO_DE_OBRA_POR_TICK = 1_000L;

    /** Ticks que dura una partida. Al llegar, la simulación se pausa. */
    public static final int DURACION_PARTIDA = 1000;

    // ---------------------------------------------------------------------------------
    // Organización (Entrega 2). Valores PROVISIONALES.
    // ---------------------------------------------------------------------------------

    /** Nombres de las compañías con que arranca la organización; el id de cada una es su posición. */
    public static final List<String> NOMBRES_COMPANIAS =
            List.of("Planta Norte", "Planta Centro", "Planta Sur");

    /** Nombres de los técnicos del fabricante, compartidos por todas las compañías. */
    private static final List<String> NOMBRES_TECNICOS_EXTERNOS = List.of("Fabricante 1", "Fabricante 2");

    /** Ticks que tarda un overhaul. La máquina no produce mientras tanto. */
    public static final int DURACION_OVERHAUL = 30;

    /**
     * Cuántas veces más tarda una reparación si el técnico no es de la especialidad del fallo.
     * Es lo que cuesta, en el modo manual, mandar a un técnico de la otra especialidad.
     */
    public static final int PENALIDAD_ESPECIALIDAD = 2;

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
                            new Sensor("Temperatura aceite", "°C", 55.0, 2.0, 70.0, 80.0, 90.0, TipoFallo.MECANICO),
                            new Sensor("Presión", "bar", 180.0, 4.0, 210.0, 230.0, 250.0, TipoFallo.MECANICO),
                            new Sensor("Vibración", "mm/s", 2.5, 0.4, 4.5, 7.1, 11.0, TipoFallo.MECANICO)),
                    60,     // intervalo preventivo en ticks
                    0.09,   // tasa base de fallo: es la máquina más crítica, falla más (× 1,5 en el 14d)
                    80,     // carga inicial
                    CAPACIDAD_LECTURAS);
            case TORNO -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura husillo", "°C", 45.0, 2.0, 60.0, 70.0, 85.0, TipoFallo.MECANICO),
                            new Sensor("Corriente motor", "A", 12.0, 0.8, 16.0, 19.0, 22.0, TipoFallo.ELECTRICO),
                            new Sensor("Vibración", "mm/s", 1.8, 0.3, 2.8, 4.5, 7.1, TipoFallo.MECANICO)),
                    80,
                    0.06,
                    70,
                    CAPACIDAD_LECTURAS);
            case ROBOT -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura motor", "°C", 40.0, 2.0, 55.0, 65.0, 80.0, TipoFallo.ELECTRICO),
                            new Sensor("Corriente", "A", 8.0, 0.6, 11.0, 13.0, 16.0, TipoFallo.ELECTRICO),
                            new Sensor("Vibración", "mm/s", 1.4, 0.25, 2.8, 4.5, 7.1, TipoFallo.MECANICO)),
                    100,
                    0.045,
                    60,
                    CAPACIDAD_LECTURAS);
            case COMPRESOR -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura descarga", "°C", 85.0, 3.0, 100.0, 110.0, 125.0, TipoFallo.MECANICO),
                            new Sensor("Presión", "bar", 7.5, 0.2, 9.0, 9.8, 11.0, TipoFallo.MECANICO),
                            new Sensor("Vibración", "mm/s", 2.0, 0.3, 4.5, 7.1, 11.0, TipoFallo.MECANICO)),
                    70,
                    0.075,
                    75,
                    CAPACIDAD_LECTURAS);
            case HORNO -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura cámara", "°C", 850.0, 10.0, 900.0, 940.0, 980.0, TipoFallo.ELECTRICO),
                            new Sensor("Temperatura carcasa", "°C", 60.0, 3.0, 80.0, 95.0, 110.0, TipoFallo.MECANICO),
                            new Sensor("Consumo de gas", "m³/h", 42.0, 2.0, 52.0, 58.0, 68.0, TipoFallo.ELECTRICO)),
                    90,
                    0.0525,
                    65,
                    CAPACIDAD_LECTURAS);
            case BOMBA -> new Maquina(idCompania, indice, etiqueta, tipo,
                    List.of(
                            new Sensor("Temperatura rodamiento", "°C", 50.0, 2.0, 65.0, 75.0, 90.0, TipoFallo.MECANICO),
                            new Sensor("Presión descarga", "bar", 6.0, 0.25, 8.0, 9.0, 10.5, TipoFallo.MECANICO),
                            new Sensor("Vibración", "mm/s", 2.2, 0.35, 4.5, 7.1, 11.0, TipoFallo.MECANICO)),
                    55,
                    0.0825,
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
     * Crea los técnicos de mantenimiento, todos libres: dos mecánicos y dos eléctricos. Son menos que
     * las máquinas, para que se formen filas.
     *
     * <p>Eran uno de cada especialidad hasta el commit 14d. Con uno solo, cada cola FIFO de técnicos
     * libres tenía a lo sumo un elemento (no se veía el orden de llegada) y las máquinas pasaban el
     * 40 % del tiempo esperando en alarma, así que ninguna decisión del jugador cambiaba el resultado.
     * El orden de la lista es el orden inicial de cada cola: Ana antes que Rosa, Luis antes que
     * Pedro.</p>
     *
     * @return lista de técnicos recién creados
     */
    public static List<Tecnico> crearTecnicos() {
        List<Tecnico> tecnicos = new ArrayList<>();
        tecnicos.add(new Tecnico("Ana", TipoFallo.MECANICO));
        tecnicos.add(new Tecnico("Luis", TipoFallo.ELECTRICO));
        tecnicos.add(new Tecnico("Rosa", TipoFallo.MECANICO));
        tecnicos.add(new Tecnico("Pedro", TipoFallo.ELECTRICO));
        return tecnicos;
    }

    /**
     * Crea los técnicos del fabricante, todos libres.
     *
     * @return lista de técnicos del fabricante recién creados
     */
    public static List<TecnicoExterno> crearTecnicosExternos() {
        List<TecnicoExterno> externos = new ArrayList<>();
        for (String nombre : NOMBRES_TECNICOS_EXTERNOS) {
            externos.add(new TecnicoExterno(nombre));
        }
        return externos;
    }
}
