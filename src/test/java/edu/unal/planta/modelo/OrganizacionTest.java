package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link Organizacion}: semillas derivadas reproducibles y ranking global consistente.
 */
@DisplayName("Organizacion")
class OrganizacionTest {

    private static final long SEMILLA_MAESTRA = 7L;
    private static final int TICKS = 300;

    @Test
    @DisplayName("la misma semilla maestra repite la simulación, y cada compañía tiene la suya")
    void mismaSemillaMismoResultado() {
        Organizacion primera = new Organizacion(SEMILLA_MAESTRA);
        Organizacion segunda = new Organizacion(SEMILLA_MAESTRA);
        for (int i = 0; i < TICKS; i++) {
            primera.tick();
            segunda.tick();
        }

        assertEquals(TICKS, primera.tickActual());
        int companias = primera.companias().size();
        for (int id = 0; id < companias; id++) {
            assertEquals(primera.compania(id).bitacora(), segunda.compania(id).bitacora(),
                    "la compañía " + id + " debe repetirse igual con la misma semilla maestra");
        }
        assertNotEquals(primera.compania(0).bitacora(), primera.compania(1).bitacora(),
                "las semillas derivadas deben ser distintas, así que las compañías no se repiten entre sí");
    }

    @Test
    @DisplayName("la misma etiqueta se puede crear en otra compañía, y entra al ranking global")
    void etiquetaUnicaSoloDentroDeLaCompania() {
        Organizacion organizacion = new Organizacion(SEMILLA_MAESTRA);
        Planta norte = organizacion.compania(0);
        Planta centro = organizacion.compania(1);

        Maquina nueva = centro.crearMaquina(TipoMaquina.PRENSA, "PRENSA-02");
        Maquina otra = norte.crearMaquina(TipoMaquina.PRENSA, "PRENSA-02");

        assertEquals(1, nueva.idCompania());
        assertEquals(0, otra.idCompania());
        assertTrue(organizacion.rankingGlobal().contains(nueva));
        assertTrue(organizacion.rankingGlobal().contains(otra));
        assertEquals(20, organizacion.rankingGlobal().size());
    }

    @Test
    @DisplayName("el modo llega a todas las compañías, incluida una creada después del cambio")
    void modoEnTodasLasCompanias() {
        Organizacion organizacion = new Organizacion(SEMILLA_MAESTRA);

        organizacion.cambiarModo(ModoSimulacion.MANUAL);
        Planta nueva = organizacion.crearCompania("Planta Oriente");

        assertTrue(organizacion.companias().stream().allMatch(c -> c.modo() == ModoSimulacion.MANUAL));
        assertEquals(ModoSimulacion.MANUAL, nueva.modo());
    }

    @Test
    @DisplayName("la consulta por puntaje mínimo coincide con filtrar el ranking completo")
    void rankingConPuntajeMinimo() {
        Organizacion organizacion = new Organizacion(SEMILLA_MAESTRA);
        for (int i = 0; i < TICKS; i++) {
            organizacion.tick();
        }
        List<Maquina> completo = organizacion.rankingGlobal();
        int minimo = completo.get(completo.size() / 3).puntajeFallos();
        assertTrue(minimo > 0, "el mínimo debe dejar fuera a alguna máquina");

        List<Maquina> esperado = completo.stream().filter(m -> m.puntajeFallos() >= minimo).toList();
        assertEquals(esperado, organizacion.rankingGlobalConPuntajeMinimo(minimo));

        Planta compania = organizacion.compania(1);
        List<Maquina> esperadoCompania = compania.rankingFallos().stream()
                .filter(m -> m.puntajeFallos() >= minimo).toList();
        assertEquals(esperadoCompania, compania.rankingConPuntajeMinimo(minimo));
    }

    @Test
    @DisplayName("el ranking global coincide con ordenar todas las máquinas, y se rehace al reiniciar")
    void rankingGlobalCoincideConOrdenarTodo() {
        Organizacion organizacion = new Organizacion(SEMILLA_MAESTRA);
        for (int i = 0; i < TICKS; i++) {
            organizacion.tick();
        }

        List<Maquina> todas = new ArrayList<>();
        for (Planta compania : organizacion.companias()) {
            todas.addAll(compania.inventario());
        }
        todas.sort(Comparator.comparingInt(Maquina::puntajeFallos).reversed()
                .thenComparingInt(Maquina::idCompania)
                .thenComparingInt(Maquina::indice));
        assertTrue(todas.get(0).puntajeFallos() > 0,
                "la simulación debe haber sumado fallos; si no, la prueba no compara nada");
        assertEquals(todas, organizacion.rankingGlobal());

        organizacion.reiniciar();
        assertEquals(todas.size(), organizacion.rankingGlobal().size());
        assertTrue(organizacion.rankingGlobal().stream().allMatch(m -> m.puntajeFallos() == 0));
    }

    @Test
    @DisplayName("una compañía creada a mitad de la simulación toma el siguiente id y el tick actual")
    void crearCompania() {
        Organizacion organizacion = new Organizacion(SEMILLA_MAESTRA);
        for (int i = 0; i < 50; i++) {
            organizacion.tick();
        }

        Planta oriente = organizacion.crearCompania("Planta Oriente");

        assertEquals(3, oriente.idCompania());
        assertEquals(Organizacion.semillaCompania(SEMILLA_MAESTRA, 3), oriente.semilla());
        assertEquals(50, oriente.tickActual());
        assertEquals(24, organizacion.rankingGlobal().size());
        assertThrows(IllegalArgumentException.class, () -> organizacion.crearCompania("  planta norte "));
        assertEquals(4, organizacion.companias().size());
    }

    @Test
    @DisplayName("la misma secuencia de ticks y creaciones repite la misma simulación")
    void creacionesReproducibles() {
        Organizacion primera = simularConCreacion();
        Organizacion segunda = simularConCreacion();

        for (int id = 0; id < primera.companias().size(); id++) {
            assertEquals(primera.compania(id).bitacora(), segunda.compania(id).bitacora());
        }
    }

    /** 50 ticks, crear "Planta Oriente" y 100 ticks más. */
    private static Organizacion simularConCreacion() {
        Organizacion organizacion = new Organizacion(SEMILLA_MAESTRA);
        for (int i = 0; i < 50; i++) {
            organizacion.tick();
        }
        organizacion.crearCompania("Planta Oriente");
        for (int i = 0; i < 100; i++) {
            organizacion.tick();
        }
        return organizacion;
    }
}
