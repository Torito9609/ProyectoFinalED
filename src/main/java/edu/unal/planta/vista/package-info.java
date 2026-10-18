/**
 * Interfaz gráfica construida con Swing.
 *
 * <p>La ventana principal reparte la pantalla en un panel por estructura de
 * datos, de modo que cada una muestre su comportamiento real: el acceso por
 * índice del arreglo dinámico, la sobrescritura de la cola circular, el orden
 * de llegada de las colas FIFO, los cuatro carriles de la cola con prioridad y
 * la cima de la pila de deshacer.</p>
 *
 * <p><strong>Reglas de este paquete:</strong></p>
 * <ul>
 *   <li>La vista solo lee el modelo y dibuja; nunca modifica el estado. Toda
 *       acción del usuario se delega al controlador.</li>
 *   <li>El acceso a las estructuras es de solo lectura: la vista recibe copias
 *       o vistas no modificables, nunca la estructura original.</li>
 *   <li>No se dibuja estado interno que {@code java.util} no expone, como la
 *       capacidad de un {@code ArrayList}. Se muestra el comportamiento del
 *       tipo abstracto, no una memoria inventada.</li>
 * </ul>
 */
package edu.unal.planta.vista;
