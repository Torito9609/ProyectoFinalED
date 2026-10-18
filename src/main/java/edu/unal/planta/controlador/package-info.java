/**
 * Controlador: reloj de la simulación y acciones del operador.
 *
 * <p>Toda acción del usuario pasa por aquí. El controlador traduce los clics de
 * la vista en llamadas a métodos del modelo, y es el dueño del reloj que hace
 * avanzar la simulación.</p>
 *
 * <p>El reloj es un {@code javax.swing.Timer}, que dispara sus eventos en el
 * hilo de despacho de Swing (EDT). Como la simulación y el dibujo ocurren en el
 * mismo hilo, no hay concurrencia ni condiciones de carrera que manejar. Por esa
 * razón el proyecto no usa {@code Thread} ni {@code java.util.Timer}.</p>
 */
package edu.unal.planta.controlador;
