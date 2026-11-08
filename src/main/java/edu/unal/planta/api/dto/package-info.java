/**
 * Objetos que viajan como JSON entre el backend y el frontend.
 *
 * <p>Todos son {@code record} inmutables que se construyen <strong>copiando</strong> el estado del
 * modelo (regla 2 de la sección 4 de {@code CLAUDE.md}): ningún endpoint devuelve un objeto del
 * modelo, y lo que reciba el frontend no puede cambiar la simulación ni cambiar con ella. Los
 * enumerados del modelo viajan como texto, con su nombre ({@code "OPERANDO"}, {@code "P1"}).</p>
 *
 * <p>Cada DTO tiene un método estático {@code desde(...)} que hace la copia. Copiar cuesta O(n)
 * en el tamaño de lo que se copia, que es pequeño: 6 máquinas de 12 lecturas, unas pocas órdenes
 * y como máximo 200 líneas de bitácora.</p>
 */
package edu.unal.planta.api.dto;
