package edu.unal.planta.vista;

import edu.unal.planta.modelo.Magnitud;

/**
 * Lo que el operador puede pedirle al sistema desde la interfaz.
 *
 * <p>Esta interfaz es la frontera entre la vista y el controlador, y está declarada <strong>en el
 * paquete de la vista</strong> a propósito. Los paneles dependen de ella, no del controlador; el
 * controlador la implementa. Así la vista no importa nada del paquete {@code controlador}, y el
 * controlador no importa nada de Swing: ninguno de los dos conoce al otro, solo comparten este
 * contrato.</p>
 *
 * <p>Es la misma idea de {@code ObservadorPlanta}, pero en el otro sentido. Allí el modelo avisa
 * hacia arriba sin saber quién escucha; aquí la vista pide hacia abajo sin saber quién responde.</p>
 *
 * <p>Nótese qué <strong>no</strong> está aquí: seleccionar una máquina o un sensor. Eso no modifica
 * la planta, solo cambia lo que el operador está mirando, así que es asunto de la vista y no tiene
 * por qué pasar por el controlador.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public interface AccionesOperador {

    /** Arranca el reloj de la simulación si está detenido, y lo detiene si está corriendo. */
    void alternarEjecucion();

    /** Avanza la simulación exactamente un tick, pausando el reloj si estaba corriendo. */
    void paso();

    /** Devuelve la planta a su estado inicial, con la misma semilla. */
    void reiniciar();

    /**
     * Para una máquina que esté operando, o arranca una que esté parada a mano.
     *
     * @param indiceMaquina posición de la máquina en el inventario
     */
    void pararOArrancar(int indiceMaquina);

    /**
     * Cambia la carga de trabajo de una máquina, que influye en su probabilidad de fallo.
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @param carga         porcentaje nuevo, entre 0 y 100
     */
    void cambiarCarga(int indiceMaquina, int carga);

    /**
     * Provoca una anomalía a mano, para forzar un caso durante la demostración.
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @param indiceSensor  sensor que se va a desviar
     * @param magnitud      gravedad del fallo
     */
    void inyectarFallo(int indiceMaquina, int indiceSensor, Magnitud magnitud);

    /** Revierte la última acción del operador, si todavía tiene sentido revertirla. */
    void deshacer();

    /**
     * Indica si el reloj de la simulación está corriendo.
     *
     * <p>La vista lo consulta para decidir si el botón dice "Iniciar" o "Pausar".</p>
     *
     * @return {@code true} si la simulación avanza sola
     */
    boolean estaCorriendo();
}
