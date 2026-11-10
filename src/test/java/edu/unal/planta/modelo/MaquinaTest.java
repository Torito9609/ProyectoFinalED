package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la recomendación de {@link Maquina}: ninguna, overhaul o reemplazo.
 *
 * <p>Están en el paquete {@code modelo} para poder usar {@code sumarPuntaje} y
 * {@code completarOverhaul}, que tienen visibilidad de paquete. Los valores se escriben en función
 * de los umbrales de {@link ConfiguracionPlanta}, así las pruebas siguen valiendo si Cristian los
 * ajusta.</p>
 */
@DisplayName("Maquina: recomendación")
class MaquinaTest {

    private static final int UMBRAL = ConfiguracionPlanta.UMBRAL_OVERHAUL;

    private Maquina maquina;

    @BeforeEach
    void crearMaquina() {
        maquina = ConfiguracionPlanta.crearMaquinas().get(0);
    }

    @Test
    @DisplayName("por debajo del umbral no se recomienda nada")
    void debajoDelUmbral() {
        maquina.sumarPuntaje(UMBRAL - 1);

        assertEquals(Recomendacion.NINGUNA, maquina.recomendacion());
    }

    @Test
    @DisplayName("justo en el umbral se recomienda un overhaul")
    void enElUmbral() {
        maquina.sumarPuntaje(UMBRAL);

        assertEquals(Recomendacion.OVERHAUL, maquina.recomendacion());
    }

    @Test
    @DisplayName("con los overhauls agotados y el umbral superado se recomienda reemplazo")
    void reemplazo() {
        completarOverhauls(ConfiguracionPlanta.MAXIMO_OVERHAULS);
        maquina.sumarPuntaje(UMBRAL);

        assertEquals(Recomendacion.REEMPLAZO, maquina.recomendacion());
    }

    @Test
    @DisplayName("con los overhauls agotados pero el puntaje bajo no se recomienda nada")
    void overhaulsAgotadosSinFallos() {
        completarOverhauls(ConfiguracionPlanta.MAXIMO_OVERHAULS);
        maquina.sumarPuntaje(UMBRAL - 1);

        assertEquals(Recomendacion.NINGUNA, maquina.recomendacion());
    }

    @Test
    @DisplayName("desgaste: en el umbral falla el doble, y cada overhaul sube la tasa un 25 %")
    void desgaste() {
        double base = maquina.probabilidadFallo();

        maquina.sumarPuntaje(UMBRAL);
        assertEquals(ConfiguracionPlanta.MAX_FACTOR_HISTORIAL * base, maquina.probabilidadFallo(), 1e-12);

        maquina.completarOverhaul();
        assertEquals((1 + ConfiguracionPlanta.INCREMENTO_TASA_POR_OVERHAUL) * base,
                maquina.probabilidadFallo(), 1e-12);
    }

    /** Simula {@code veces} overhauls; cada uno deja el puntaje en 0. */
    private void completarOverhauls(int veces) {
        for (int i = 0; i < veces; i++) {
            maquina.sumarPuntaje(UMBRAL);
            maquina.completarOverhaul();
        }
        assertEquals(0, maquina.puntajeFallos());
        assertEquals(veces, maquina.numeroOverhauls());
    }
}
