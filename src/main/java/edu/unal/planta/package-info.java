/**
 * Raíz del simulador de monitoreo y mantenimiento de planta.
 *
 * <p>El proyecto sigue el patrón Modelo-Vista-Controlador y se reparte en
 * cuatro paquetes:</p>
 *
 * <ul>
 *   <li>{@link edu.unal.planta.estructuras}: estructuras de datos genéricas,
 *       sin ninguna dependencia del dominio.</li>
 *   <li>{@link edu.unal.planta.modelo}: dominio y simulación. No conoce la vista.</li>
 *   <li>{@link edu.unal.planta.vista}: interfaz Swing. Solo lee el modelo y dibuja.</li>
 *   <li>{@link edu.unal.planta.controlador}: traduce las acciones del usuario y el
 *       reloj de la simulación en llamadas al modelo.</li>
 * </ul>
 *
 * <p>La dependencia entre paquetes va en un solo sentido: vista y controlador
 * conocen el modelo, pero el modelo no conoce a ninguno de los dos. Gracias a
 * eso la simulación podría ejecutarse sin interfaz gráfica.</p>
 */
package edu.unal.planta;
