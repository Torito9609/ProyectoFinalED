package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link RankingFallos}.
 *
 * <p>Usa las seis máquinas de la configuración. Sin fallos, todas tienen puntaje 0 y el ranking
 * queda en orden alfabético de etiqueta: BOMBA-01, COMPRES-01, HORNO-01, PRENSA-01, ROBOT-01,
 * TORNO-01.</p>
 */
@DisplayName("RankingFallos")
class RankingFallosTest {

    private List<Maquina> maquinas;
    private RankingFallos ranking;

    @BeforeEach
    void crearRanking() {
        maquinas = ConfiguracionPlanta.crearMaquinas();
        ranking = new RankingFallos(maquinas);
    }

    @Test
    @DisplayName("un fallo lleva la máquina al primer lugar y las demás siguen por etiqueta")
    void sumarFalloReordena() {
        ranking.sumarFallo(maquina("TORNO-01"), 3);

        assertEquals(List.of("TORNO-01", "BOMBA-01", "COMPRES-01", "HORNO-01", "PRENSA-01", "ROBOT-01"),
                etiquetas(ranking.enOrden()));
        assertEquals(3, maquina("TORNO-01").puntajeFallos());
    }

    @Test
    @DisplayName("dos máquinas con el mismo puntaje se quedan las dos, desempatadas por etiqueta")
    void empateConservaAmbas() {
        ranking.sumarFallo(maquina("TORNO-01"), 2);
        ranking.sumarFallo(maquina("PRENSA-01"), 2);

        assertEquals(6, ranking.tamano());
        assertEquals(List.of("PRENSA-01", "TORNO-01"), etiquetas(ranking.enOrden()).subList(0, 2));
    }

    @Test
    @DisplayName("la consulta por umbral devuelve solo las que lo alcanzan, en orden")
    void conPuntajeMinimo() {
        ranking.sumarFallo(maquina("PRENSA-01"), 5);
        ranking.sumarFallo(maquina("TORNO-01"), 3);
        ranking.sumarFallo(maquina("ROBOT-01"), 1);

        assertEquals(List.of("PRENSA-01", "TORNO-01"), etiquetas(ranking.conPuntajeMinimo(3)));
    }

    private Maquina maquina(String etiqueta) {
        return maquinas.stream().filter(m -> m.etiqueta().equals(etiqueta)).findFirst().orElseThrow();
    }

    private static List<String> etiquetas(List<Maquina> lista) {
        return lista.stream().map(Maquina::etiqueta).toList();
    }
}
