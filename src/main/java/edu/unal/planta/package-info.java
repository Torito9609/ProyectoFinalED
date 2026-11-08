/**
 * Raíz del simulador de monitoreo y mantenimiento de planta (backend).
 *
 * <p>El proyecto sigue el patrón Modelo-Vista-Controlador, con la vista en otro
 * repositorio: un frontend web que consume la API REST de este. Aquí el código
 * se reparte en cuatro paquetes:</p>
 *
 * <ul>
 *   <li>{@link edu.unal.planta.estructuras}: estructuras de datos genéricas,
 *       sin ninguna dependencia del dominio.</li>
 *   <li>{@link edu.unal.planta.modelo}: dominio y simulación. Java puro: no
 *       conoce la vista ni el framework.</li>
 *   <li>{@link edu.unal.planta.servicio}: el único dueño del modelo. Serializa el acceso y
 *       lleva el reloj de la simulación.</li>
 *   <li>{@code api}: los controladores REST, que traducen las peticiones del
 *       frontend en llamadas al servicio y responden con DTOs.</li>
 * </ul>
 *
 * <p>La dependencia entre paquetes va en un solo sentido: la API conoce el
 * servicio y el servicio conoce el modelo, pero el modelo no conoce a ninguno
 * de los dos. Gracias a eso la simulación se prueba completa con JUnit, sin
 * levantar un servidor.</p>
 */
package edu.unal.planta;
