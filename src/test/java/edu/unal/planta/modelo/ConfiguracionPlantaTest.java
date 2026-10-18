package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la configuración inicial de la planta.
 *
 * <p>Las entidades del modelo son datos y no necesitan pruebas propias. Lo que sí conviene probar
 * es que los valores provisionales de la configuración cumplan las condiciones del documento de
 * diseño: un error ahí no rompe el programa, pero arruinaría la demostración.</p>
 */
@DisplayName("ConfiguracionPlanta")
class ConfiguracionPlantaTest {

    @Test
    @DisplayName("la planta arranca con seis máquinas operando y menos técnicos que máquinas")
    void inventarioInicial() {
        List<Maquina> maquinas = ConfiguracionPlanta.crearMaquinas();
        List<Tecnico> tecnicos = ConfiguracionPlanta.crearTecnicos();

        assertEquals(6, maquinas.size());
        for (Maquina maquina : maquinas) {
            assertEquals(EstadoMaquina.OPERANDO, maquina.estado());
            assertEquals(ConfiguracionPlanta.CAPACIDAD_LECTURAS, maquina.lecturas().capacidad());
        }
        assertTrue(tecnicos.size() < maquinas.size(),
                "debe haber menos técnicos que máquinas para que se formen filas");
        assertTrue(tecnicos.stream().allMatch(Tecnico::estaLibre));
    }

    @Test
    @DisplayName("ningún sensor puede dar alarmas falsas por puro ruido")
    void sinAlarmasFalsas() {
        for (Maquina maquina : ConfiguracionPlanta.crearMaquinas()) {
            for (Sensor sensor : maquina.sensores()) {
                double peorCaso = sensor.nominal() + sensor.ruido();
                assertTrue(peorCaso < sensor.umbralAdvertencia(),
                        maquina.etiqueta() + " / " + sensor.nombre() + ": el peor caso del ruido es "
                                + peorCaso + " y el umbral de advertencia es " + sensor.umbralAdvertencia());
            }
        }
    }

    @Test
    @DisplayName("cada magnitud deja la lectura en su zona de umbrales, pese al ruido del sensor")
    void cadaMagnitudCaeEnSuZona() {
        GeneradorFallos generador = new GeneradorFallos(ConfiguracionPlanta.SEMILLA_POR_DEFECTO);

        for (Maquina maquina : ConfiguracionPlanta.crearMaquinas()) {
            for (int indice = 0; indice < maquina.sensores().size(); indice++) {
                Sensor sensor = maquina.sensor(indice);
                for (Magnitud magnitud : Magnitud.values()) {
                    double desplazamiento =
                            generador.crear(maquina, indice, magnitud).desplazamiento();
                    double peorAbajo = sensor.nominal() - sensor.ruido() + desplazamiento;
                    double peorArriba = sensor.nominal() + sensor.ruido() + desplazamiento;
                    String donde = maquina.etiqueta() + " / " + sensor.nombre() + " / " + magnitud;

                    switch (magnitud) {
                        case LEVE -> {
                            assertTrue(peorAbajo >= sensor.umbralAdvertencia(),
                                    donde + ": debe alcanzar la advertencia incluso con el ruido en contra");
                            assertTrue(peorArriba < sensor.umbralAlto(),
                                    donde + ": no debe llegar al umbral alto");
                        }
                        case MODERADA -> {
                            assertTrue(peorAbajo >= sensor.umbralAlto(),
                                    donde + ": debe alcanzar el umbral alto");
                            assertTrue(peorArriba < sensor.umbralCritico(),
                                    donde + ": no debe llegar al umbral crítico");
                        }
                        case SEVERA -> assertTrue(peorAbajo >= sensor.umbralCritico(),
                                donde + ": debe superar el umbral crítico");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("toda prioridad tiene una duración de reparación positiva")
    void duracionesValidas() {
        for (Prioridad prioridad : Prioridad.values()) {
            assertTrue(ConfiguracionPlanta.duracionReparacion(prioridad) > 0,
                    "la prioridad " + prioridad + " necesita una duración positiva");
        }
    }
}
