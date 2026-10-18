package edu.unal.planta.controlador;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Magnitud;
import edu.unal.planta.modelo.Planta;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del controlador.
 *
 * <p>Son pocas y deliberadamente no tocan el reloj: {@code javax.swing.Timer} es de la librería
 * estándar y probarlo sería probar a Swing, no a nuestro código. Lo que sí se prueba es que cada
 * acción del operador produzca en el modelo exactamente el efecto esperado.</p>
 */
@DisplayName("ControladorPlanta")
class ControladorPlantaTest {

    private static final long SEMILLA = 7L;

    @Test
    @DisplayName("un paso avanza exactamente un tick")
    void unPasoAvanzaUnTick() {
        Planta planta = new Planta(SEMILLA);
        ControladorPlanta controlador = new ControladorPlanta(planta);

        controlador.paso();
        controlador.paso();
        controlador.paso();

        assertEquals(3, planta.tickActual());
        assertFalse(controlador.estaCorriendo(), "dar un paso deja el reloj detenido");
    }

    @Test
    @DisplayName("reiniciar devuelve la planta al estado inicial y repite la misma simulación")
    void reiniciarRepiteLaSimulacion() {
        Planta planta = new Planta(SEMILLA);
        ControladorPlanta controlador = new ControladorPlanta(planta);

        for (int i = 0; i < 40; i++) {
            controlador.paso();
        }
        List<String> primeraVuelta = loQuePaso(planta);

        controlador.reiniciar();
        assertEquals(0, planta.tickActual());
        assertTrue(loQuePaso(planta).isEmpty(), "reiniciar limpia la bitácora");

        for (int i = 0; i < 40; i++) {
            controlador.paso();
        }

        assertEquals(primeraVuelta, loQuePaso(planta),
                "con la misma semilla, reiniciar y repetir produce la misma simulación");
    }

    /**
     * Devuelve lo que ocurrió en la simulación, sin la primera línea de la bitácora.
     *
     * <p>Esa primera línea dice "Planta iniciada" o "Planta reiniciada" según el caso, y es la
     * única diferencia legítima entre arrancar y reiniciar: no forma parte de lo que la semilla
     * tiene que reproducir.</p>
     *
     * @param planta planta consultada
     * @return las líneas de la bitácora a partir de la segunda
     */
    private static List<String> loQuePaso(Planta planta) {
        List<String> bitacora = planta.bitacora();
        return bitacora.subList(1, bitacora.size());
    }

    @Test
    @DisplayName("parar y arrancar una máquina se puede deshacer")
    void pararYDeshacer() {
        Planta planta = new Planta(SEMILLA);
        ControladorPlanta controlador = new ControladorPlanta(planta);

        controlador.pararOArrancar(0);
        assertEquals(EstadoMaquina.PARADA_MANUAL, planta.maquina(0).estado());
        assertEquals(1, planta.accionesDeshacibles().size());

        controlador.deshacer();
        assertEquals(EstadoMaquina.OPERANDO, planta.maquina(0).estado());
        assertEquals(0, planta.accionesDeshacibles().size());
    }

    @Test
    @DisplayName("las acciones que no aplican se ignoran en vez de fallar")
    void accionesQueNoAplican() {
        Planta planta = new Planta(SEMILLA);
        ControladorPlanta controlador = new ControladorPlanta(planta);

        // Deshacer con la pila vacía no debe romper nada.
        assertDoesNotThrow(controlador::deshacer);

        // Una máquina que un técnico está atendiendo no se puede parar a mano.
        controlador.inyectarFallo(0, 0, Magnitud.SEVERA);
        controlador.paso();
        assertNotEquals(EstadoMaquina.OPERANDO, planta.maquina(0).estado());

        EstadoMaquina antes = planta.maquina(0).estado();
        assertDoesNotThrow(() -> controlador.pararOArrancar(0));
        assertEquals(antes, planta.maquina(0).estado(), "la acción no aplicaba, así que se ignoró");
    }
}
