package edu.unal.planta.modelo;

/**
 * Prioridad de una orden de trabajo, de la más urgente a la menos urgente.
 *
 * <p>El esquema de cuatro niveles está inspirado en la norma <strong>ISA-18.2</strong> de gestión de
 * alarmas: la prioridad combina la severidad de la condición con el tiempo disponible para
 * responder. Los tres primeros niveles corresponden a alarmas detectadas en los sensores; el cuarto
 * corresponde al mantenimiento preventivo, que no es un fallo sino un trabajo programado.</p>
 *
 * <h2>Relación con la cola con prioridad</h2>
 * <p>El campo {@code nivel} es, a propósito, igual a la posición de la constante dentro del enum.
 * {@link ColaPrioridadOrdenes} usa ese número como <strong>índice de su arreglo de colas</strong>,
 * así que insertar una orden es un acceso directo O(1). Un bloque estático verifica esa igualdad al
 * cargar la clase: si alguien reordena las constantes y olvida corregir los niveles, el programa
 * falla de inmediato en vez de asignar las órdenes al carril equivocado.</p>
 *
 * <h2>Sobre el color</h2>
 * <p>El color se guarda como un entero <strong>RGB</strong> en formato {@code 0xRRGGBB}, no como un
 * {@code java.awt.Color}. La razón es la regla de arquitectura del proyecto: ninguna clase del
 * modelo importa {@code java.awt} ni {@code javax.swing}. La vista convierte este número en un
 * color cuando lo necesita. Así el modelo define <em>qué</em> color le corresponde a cada prioridad
 * sin depender de la tecnología que lo dibuje.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public enum Prioridad {

    /** Prioridad máxima: la lectura superó el umbral crítico y la máquina debe detenerse ya. */
    P1(0, "CRÍTICA", "Paro inmediato de la máquina", 0xC62828),

    /** La lectura superó el umbral alto: hay que atenderla dentro del turno. */
    P2(1, "ALTA", "Atender en el turno", 0xEF6C00),

    /** La lectura superó el umbral de advertencia: conviene ir a revisar. */
    P3(2, "MEDIA", "Ir a revisar", 0xF9A825),

    /** No hay fallo: se cumplió el intervalo de mantenimiento preventivo. */
    P4(3, "BAJA", "Mantenimiento programado", 0x1565C0);

    /** Cantidad de niveles de prioridad; es el tamaño del arreglo de colas. */
    public static final int NIVELES = values().length;

    static {
        // El arreglo de ColaPrioridadOrdenes se indexa con el nivel, así que el nivel declarado
        // debe coincidir con la posición de la constante. Se verifica al cargar la clase.
        for (Prioridad prioridad : values()) {
            if (prioridad.nivel != prioridad.ordinal()) {
                throw new IllegalStateException("El nivel de " + prioridad.name()
                        + " es " + prioridad.nivel + " pero su posición en el enum es "
                        + prioridad.ordinal() + ". Deben ser iguales.");
            }
        }
    }

    private final int nivel;
    private final String descripcion;
    private final String accion;
    private final int rgb;

    /**
     * Crea una prioridad.
     *
     * @param nivel      posición en el orden de atención; 0 es la más urgente
     * @param descripcion nombre legible del nivel
     * @param accion     qué debe hacer el operador ante una orden de esta prioridad
     * @param rgb        color asociado, en formato {@code 0xRRGGBB}
     */
    Prioridad(int nivel, String descripcion, String accion, int rgb) {
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.accion = accion;
        this.rgb = rgb;
    }

    /**
     * Devuelve el nivel de atención, donde 0 es el más urgente.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return nivel entre 0 y {@code NIVELES - 1}
     */
    public int nivel() {
        return nivel;
    }

    /**
     * Devuelve el nombre legible del nivel, por ejemplo {@code "CRÍTICA"}.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return descripción del nivel
     */
    public String descripcion() {
        return descripcion;
    }

    /**
     * Devuelve la acción que corresponde a esta prioridad.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return acción recomendada para el operador
     */
    public String accion() {
        return accion;
    }

    /**
     * Devuelve el color asociado a la prioridad, en formato {@code 0xRRGGBB}.
     *
     * <p>La vista lo convierte a un color de Swing; el modelo no conoce esa clase.</p>
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return color empacado en un entero
     */
    public int rgb() {
        return rgb;
    }

    /**
     * Devuelve la prioridad que corresponde a un nivel.
     *
     * <p>Complejidad: O(1), porque el nivel es la posición en el arreglo de constantes.</p>
     *
     * @param nivel nivel buscado, entre 0 y {@code NIVELES - 1}
     * @return la prioridad de ese nivel
     * @throws IllegalArgumentException si el nivel está fuera de rango
     */
    public static Prioridad porNivel(int nivel) {
        if (nivel < 0 || nivel >= NIVELES) {
            throw new IllegalArgumentException(
                    "Nivel de prioridad fuera de rango: " + nivel + ". Debe estar entre 0 y " + (NIVELES - 1));
        }
        return values()[nivel];
    }

    /**
     * Devuelve una etiqueta corta para la interfaz, por ejemplo {@code "P1 CRÍTICA"}.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @return nombre de la constante seguido de su descripción
     */
    public String etiqueta() {
        return name() + " " + descripcion;
    }
}
