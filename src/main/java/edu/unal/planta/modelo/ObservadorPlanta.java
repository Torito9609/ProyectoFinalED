package edu.unal.planta.modelo;

/**
 * Quien quiera enterarse de que el estado de la planta cambió.
 *
 * <p>Es la pieza que sostiene la regla de arquitectura más importante del proyecto: <strong>el
 * modelo no conoce la vista</strong>. Al terminar cada tick, {@link Planta} avisa a sus
 * observadores a través de esta interfaz, sin saber quiénes son ni qué hacen con el aviso. En la
 * aplicación el observador es la ventana de Swing, que se vuelve a dibujar; en las pruebas es un
 * contador que verifica cuántas veces avisó; y podría ser una salida por consola sin tocar una
 * línea del modelo.</p>
 *
 * <p>Es el patrón Observador, y la razón por la que el paquete {@code modelo} no importa nada de
 * Swing.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public interface ObservadorPlanta {

    /**
     * Avisa que la planta cambió de estado y que conviene volver a leerla.
     *
     * @param planta la planta que cambió
     */
    void plantaCambio(Planta planta);
}
