package edu.unal.planta.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del servicio de simulación, sin HTTP y sin Spring.
 *
 * <p>El reloj se simula llamando a mano a {@link ServicioSimulacion#alDispararReloj()}, que es lo
 * que haría Spring en cada disparo. Así las pruebas no dependen del tiempo real.</p>
 */
@DisplayName("ServicioSimulacion")
class ServicioSimulacionTest {

    private ServicioSimulacion servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ServicioSimulacion(7L);
    }

    @Test
    @DisplayName("arranca en pausa, en el tick 0 y con la semilla indicada")
    void estadoInicial() {
        assertFalse(servicio.enEjecucion());
        assertEquals(0, servicio.tickActual());
        assertEquals(7L, servicio.semilla());
    }

    @Test
    @DisplayName("en pausa, el reloj no avanza la simulación")
    void relojEnPausa() {
        servicio.alDispararReloj();
        servicio.alDispararReloj();

        assertEquals(0, servicio.tickActual());
    }

    @Test
    @DisplayName("en ejecución, cada disparo del reloj avanza un tick, y al pausar se detiene")
    void relojEnEjecucion() {
        servicio.iniciar();
        servicio.alDispararReloj();
        servicio.alDispararReloj();
        assertEquals(2, servicio.tickActual());

        servicio.pausar();
        servicio.alDispararReloj();
        assertEquals(2, servicio.tickActual());
    }

    @Test
    @DisplayName("paso avanza exactamente un tick y deja la simulación en pausa")
    void paso() {
        servicio.iniciar();
        servicio.paso();

        assertEquals(1, servicio.tickActual());
        assertFalse(servicio.enEjecucion());
    }

    @Test
    @DisplayName("reiniciar vuelve al tick 0, en pausa y con la misma semilla")
    void reiniciar() {
        servicio.iniciar();
        servicio.alDispararReloj();
        servicio.alDispararReloj();
        assertTrue(servicio.enEjecucion());

        servicio.reiniciar();

        assertEquals(0, servicio.tickActual());
        assertFalse(servicio.enEjecucion());
        assertEquals(7L, servicio.semilla());
    }

    @Test
    @DisplayName("la foto del estado refleja el tick, la semilla y si está en ejecución")
    void estadoPlanta() {
        servicio.paso();
        servicio.iniciar();

        var estado = servicio.estadoPlanta();

        assertEquals(1, estado.tick());
        assertEquals(7L, estado.semilla());
        assertTrue(estado.enEjecucion());
    }
}
