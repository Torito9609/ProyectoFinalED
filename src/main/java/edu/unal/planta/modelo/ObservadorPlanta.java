package edu.unal.planta.modelo;

/**
 * Quien quiera enterarse de que el estado de la planta cambió.
 *
 * <p>Es la pieza que sostiene la regla de arquitectura más importante del proyecto: <strong>el
 * modelo no conoce la vista</strong>. Al terminar cada tick, {@link Planta} avisa a sus
 * observadores a través de esta interfaz, sin saber quiénes son ni qué hacen con el aviso. En la
 * Entrega 1 el observador era la ventana de Swing, que se volvía a dibujar; en las pruebas es un
 * contador que verifica cuántas veces avisó.</p>
 *
 * <p>Desde la Entrega 2 la aplicación no lo necesita: el frontend web consulta el estado cuando lo
 * quiere, a través de la API. La interfaz se conserva porque es la que demuestra que el modelo no
 * depende de quién lo use: el paquete {@code modelo} no importa nada de la vista ni del framework.
 * Es el patrón Observador.</p>
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
