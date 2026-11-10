package edu.unal.planta.estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link ArbolSegmentos}.
 *
 * <p>Cubre los casos del plan: el rango completo, un rango de un elemento, un máximo a mitad del
 * rango y una actualización. El árbol de prueba tiene tamaño 6, que no es potencia de 2, para que
 * existan hojas de relleno y se compruebe que no alteran los resultados.</p>
 */
@DisplayName("ArbolSegmentos")
class ArbolSegmentosTest {

    /** Valores de prueba: suma 18, máximo 7 en la posición 2. */
    private static final int[] VALORES = {3, 1, 7, 0, 2, 5};

    private ArbolSegmentos arbol;

    @BeforeEach
    void crearArbol() {
        arbol = new ArbolSegmentos(VALORES.length);
        for (int i = 0; i < VALORES.length; i++) {
            arbol.actualizar(i, VALORES[i]);
        }
    }

    @Test
    @DisplayName("el rango completo da la suma y el máximo de todos los valores")
    void rangoCompleto() {
        assertEquals(18, arbol.suma(0, 5));
        assertEquals(7, arbol.maximo(0, 5));
    }

    @Test
    @DisplayName("un rango de un solo elemento devuelve ese elemento")
    void rangoDeUnElemento() {
        assertEquals(0, arbol.suma(3, 3));
        assertEquals(0, arbol.maximo(3, 3));
    }

    @Test
    @DisplayName("encuentra un máximo que está a mitad del rango")
    void maximoAMitadDelRango() {
        assertEquals(10, arbol.suma(1, 4));
        assertEquals(7, arbol.maximo(1, 4));
    }

    @Test
    @DisplayName("una actualización cambia la suma, el máximo y el valor de la posición")
    void actualizacion() {
        arbol.actualizar(2, 1);

        assertEquals(1, arbol.valor(2));
        assertEquals(12, arbol.suma(0, 5));
        assertEquals(5, arbol.maximo(0, 5));
    }
}
