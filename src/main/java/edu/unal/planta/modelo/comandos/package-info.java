/**
 * Acciones del operador que se pueden deshacer, siguiendo el patrón Comando.
 *
 * <p>Cada comando guarda lo necesario para revertirse y se apila en el momento
 * de ejecutarse. Deshacer es sacar el elemento de la cima de la pila: la última
 * acción es la primera en revertirse (LIFO).</p>
 *
 * <p>No todas las acciones son deshacibles. Clasificar un evento, asignar una
 * orden o inyectar un fallo no lo son, porque revertirlas obligaría a reinsertar
 * un elemento por el frente de una cola FIFO o a sacar de la cola con prioridad
 * un elemento que no es el siguiente. Ambas cosas rompen el tipo abstracto de
 * datos, y preferimos respetarlo antes que ofrecer la funcionalidad.</p>
 *
 * <p>Un comando solo se revierte si el estado actual sigue siendo el que el
 * comando dejó. Si la simulación ya lo cambió por su cuenta, el comando se
 * descarta y queda registrado en la bitácora.</p>
 */
package edu.unal.planta.modelo.comandos;
