package edu.unal.planta.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.unal.planta.servicio.ServicioSimulacion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas HTTP del ranking de fallos.
 *
 * <p>Cada prueba reinicia la simulación y avanza 300 ticks: con la semilla fija el resultado es
 * siempre el mismo, y las máquinas ya acumularon puntaje, así que el orden sí se pone a prueba.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ControladorSuperusuario")
class ControladorSuperusuarioTest {

    private static final int TICKS = 300;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void simular() {
        servicio.reiniciar();
        for (int i = 0; i < TICKS; i++) {
            servicio.paso();
        }
    }

    @Test
    @DisplayName("el ranking global trae las 18 máquinas, de mayor a menor puntaje")
    void rankingGlobal() throws Exception {
        String json = mvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcance").value("Global"))
                .andExpect(jsonPath("$.umbralOverhaul").value(12))
                .andExpect(jsonPath("$.maquinas.length()").value(18))
                .andExpect(jsonPath("$.maquinas[0].posicion").value(1))
                .andReturn().getResponse().getContentAsString();

        List<Integer> puntajes = JsonPath.read(json, "$.maquinas[*].puntajeFallos");
        assertTrue(puntajes.get(0) > 0, "la simulación debe haber sumado fallos");
        for (int i = 1; i < puntajes.size(); i++) {
            assertTrue(puntajes.get(i - 1) >= puntajes.get(i), "el ranking debe ir de mayor a menor");
        }
    }

    @Test
    @DisplayName("con compañía, trae solo las máquinas de esa compañía")
    void rankingDeCompania() throws Exception {
        String json = mvc.perform(get("/api/ranking").param("compania", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcance").value("Planta Centro"))
                .andExpect(jsonPath("$.maquinas.length()").value(6))
                .andReturn().getResponse().getContentAsString();

        List<Integer> companias = JsonPath.read(json, "$.maquinas[*].idCompania");
        assertTrue(companias.stream().allMatch(id -> id == 1));
    }

    @Test
    @DisplayName("con puntaje mínimo, trae solo las máquinas que lo alcanzan")
    void puntajeMinimo() throws Exception {
        String global = mvc.perform(get("/api/ranking")).andReturn().getResponse().getContentAsString();
        List<Integer> todos = JsonPath.read(global, "$.maquinas[*].puntajeFallos");
        int minimo = todos.get(5);
        long esperadas = todos.stream().filter(p -> p >= minimo).count();

        String json = mvc.perform(get("/api/ranking").param("puntajeMinimo", String.valueOf(minimo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> puntajes = JsonPath.read(json, "$.maquinas[*].puntajeFallos");
        assertEquals(esperadas, puntajes.size());
        assertTrue(puntajes.stream().allMatch(p -> p >= minimo));
    }

    @Test
    @DisplayName("una compañía que no existe responde 404")
    void companiaInexistente() throws Exception {
        mvc.perform(get("/api/ranking").param("compania", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("las máquinas de una compañía traen puntaje, overhauls y recomendación")
    void maquinaConRecomendacion() throws Exception {
        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.maquinas[0].puntajeFallos").isNumber())
                .andExpect(jsonPath("$.maquinas[0].numeroOverhauls").value(0))
                .andExpect(jsonPath("$.maquinas[0].recomendacion").isString());
    }
}
