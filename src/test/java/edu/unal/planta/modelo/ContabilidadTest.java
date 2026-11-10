package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link Contabilidad}: la caja, los acumulados y la quiebra.
 */
@DisplayName("Contabilidad")
class ContabilidadTest {

    @Test
    @DisplayName("ingresar y cobrar mueven la caja, y bajar de 0 es quiebra")
    void cajaYQuiebra() {
        Contabilidad contabilidad = new Contabilidad(1_000);

        contabilidad.ingresar(500);
        contabilidad.iniciarTick();
        contabilidad.cobrar(1_200);

        assertEquals(300, contabilidad.caja());
        assertEquals(500, contabilidad.ingresos());
        assertEquals(1_200, contabilidad.costos());
        assertEquals(1_200, contabilidad.costoDelTick());
        assertFalse(contabilidad.enQuiebra());

        contabilidad.cobrar(301);
        assertTrue(contabilidad.enQuiebra());

        contabilidad.reiniciar();
        assertEquals(1_000, contabilidad.caja());
        assertFalse(contabilidad.enQuiebra());
    }
}
