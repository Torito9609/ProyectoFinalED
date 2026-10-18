/**
 * Dominio y simulación de la planta.
 *
 * <p>Contiene las entidades (máquinas, sensores, lecturas, eventos y órdenes de
 * trabajo), las estructuras donde se guardan y la lógica que avanza la
 * simulación un tick a la vez.</p>
 *
 * <p><strong>Regla de arquitectura:</strong> ninguna clase de este paquete
 * importa {@code javax.swing} ni {@code java.awt}. Cuando el estado cambia, el
 * modelo avisa a sus observadores mediante una interfaz propia; quién escuche y
 * cómo lo dibuje no es asunto suyo. Esa separación es la que permite probar la
 * simulación completa con JUnit, sin abrir una ventana.</p>
 *
 * <p>La aleatoriedad proviene de un único generador con semilla configurable,
 * de modo que la misma semilla reproduce exactamente la misma simulación.</p>
 */
package edu.unal.planta.modelo;
