package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unal.planta.modelo.comandos.ComandoOperador;
import edu.unal.planta.modelo.comandos.PararOArrancarMaquina;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la simulación completa, sin interfaz gráfica.
 *
 * <p>Son pocas y cada una cubre una cadena completa de pasos del tick, en vez de probar métodos
 * sueltos. Varias apagan la carga de todas las máquinas para que no aparezcan fallos aleatorios: la
 * probabilidad de fallo es proporcional a la carga, así que con carga en cero lo único que ocurre
 * es lo que la prueba provoca a propósito.</p>
 */
@DisplayName("Planta")
class PlantaTest {

    private static final long SEMILLA = 7L;
    private static final int PRENSA = 0;
    private static final int TORNO = 1;
    private static final int VIBRACION = 2;

    @Test
    @DisplayName("la misma semilla produce exactamente la misma simulación")
    void mismaSemillaMismaSimulacion() {
        Planta primera = new Planta(SEMILLA);
        Planta segunda = new Planta(SEMILLA);
        Planta distinta = new Planta(SEMILLA + 1);

        for (int i = 0; i < 150; i++) {
            primera.tick();
            segunda.tick();
            distinta.tick();
        }

        assertEquals(primera.bitacora(), segunda.bitacora(),
                "con la misma semilla la simulación debe repetirse igual, para poder ensayar la demo");
        assertNotEquals(primera.bitacora(), distinta.bitacora(),
                "con otra semilla la simulación debe ser distinta");
    }

    @Test
    @DisplayName("una anomalía severa termina en una orden P1 asignada a un técnico")
    void anomaliaSeveraProduceP1() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);

        planta.tick();

        Optional<OrdenTrabajo> asignada = ordenEnCurso(planta);
        assertTrue(asignada.isPresent(), "la orden debió asignarse a un técnico");
        assertEquals(Prioridad.P1, asignada.orElseThrow().prioridad(),
                "una lectura sobre el umbral crítico debe clasificarse como P1");
        assertEquals(PRENSA, asignada.orElseThrow().indiceMaquina());
        assertNotEquals(EstadoMaquina.OPERANDO, planta.maquina(PRENSA).estado(),
                "una alarma crítica saca la máquina de operación");
    }

    @Test
    @DisplayName("se clasifica un solo evento por tick, para que la cola FIFO se pueda ver")
    void unEventoPorTick() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.inyectarFallo(TORNO, VIBRACION, Magnitud.SEVERA);

        planta.tick();

        assertEquals(1, planta.eventosPendientes().size(),
                "se detectaron dos eventos y solo debió clasificarse uno");
    }

    @Test
    @DisplayName("cuando el técnico termina, la máquina vuelve a operar sin anomalía")
    void elTecnicoDevuelveLaMaquinaAOperando() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);

        // Un tick para detectar, clasificar y asignar, más la duración de una reparación P1.
        int ticksNecesarios = 1 + ConfiguracionPlanta.duracionReparacion(Prioridad.P1);
        for (int i = 0; i < ticksNecesarios; i++) {
            planta.tick();
        }

        Maquina prensa = planta.maquina(PRENSA);
        assertEquals(EstadoMaquina.OPERANDO, prensa.estado());
        assertFalse(prensa.tieneAnomalia(), "la reparación quita el fallo físico");
        assertFalse(prensa.alarmaActiva(), "y también la alarma");
        assertEquals(0, prensa.ticksDesdeMantenimiento(), "el mantenimiento queda al día");
    }

    @Test
    @DisplayName("el preventivo genera una sola orden P4 por máquina, no una por tick")
    void preventivoNoSeRepite() {
        Planta planta = plantaSinFallosAleatorios();
        Maquina prensa = planta.maquina(PRENSA);

        // Suficientes ticks para pasar el intervalo preventivo de la primera máquina.
        for (int i = 0; i < prensa.intervaloPreventivo() + 10; i++) {
            planta.tick();
        }

        long ordenesPreventivas = planta.bitacora().stream()
                .filter(linea -> linea.contains("Preventivo:"))
                .count();

        assertEquals(1, ordenesPreventivas,
                "el indicador de preventiva pendiente debe evitar una orden nueva en cada tick");
    }

    @Test
    @DisplayName("deshacer se descarta si la simulación ya cambió el estado")
    void deshacerSeDescartaSiElEstadoCambio() {
        Planta planta = plantaSinFallosAleatorios();
        Maquina prensa = planta.maquina(PRENSA);

        ComandoOperador parar = new PararOArrancarMaquina(prensa);
        planta.ejecutar(parar);
        assertEquals(EstadoMaquina.PARADA_MANUAL, prensa.estado());

        // La simulación cambia el estado por su cuenta, como lo haría una alarma crítica
        // o un técnico que termina un trabajo.
        prensa.cambiarEstado(EstadoMaquina.DETENIDA);
        assertFalse(parar.sePuedeDeshacer());

        planta.deshacer();

        assertEquals(EstadoMaquina.DETENIDA, prensa.estado(),
                "revertir aquí dejaría un estado inconsistente, así que el comando se descarta");
        assertTrue(planta.bitacora().stream().anyMatch(linea -> linea.contains("Descartado")),
                "el descarte debe quedar registrado en la bitácora");
        assertTrue(planta.accionesDeshacibles().isEmpty(), "el comando salió de la pila");
    }

    /**
     * Crea una planta donde ninguna máquina falla por azar, poniendo su carga en cero.
     *
     * @return planta lista para que la prueba provoque solo lo que le interesa
     */
    private static Planta plantaSinFallosAleatorios() {
        Planta planta = new Planta(SEMILLA);
        for (Maquina maquina : planta.inventario()) {
            maquina.cambiarCarga(0);
        }
        return planta;
    }

    /**
     * Busca la orden que algún técnico esté atendiendo.
     *
     * @param planta planta consultada
     * @return la primera orden en curso, si hay alguna
     */
    private static Optional<OrdenTrabajo> ordenEnCurso(Planta planta) {
        return planta.tecnicos().stream()
                .map(Tecnico::ordenActual)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
