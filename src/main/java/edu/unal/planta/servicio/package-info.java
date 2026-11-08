/**
 * Servicio de simulación: el único dueño del modelo.
 *
 * <p>Guarda la planta, serializa el acceso con métodos {@code synchronized} y lleva el reloj de la
 * simulación. Es el único paquete, junto con {@code api}, que usa Spring; el modelo no lo conoce.</p>
 */
package edu.unal.planta.servicio;
