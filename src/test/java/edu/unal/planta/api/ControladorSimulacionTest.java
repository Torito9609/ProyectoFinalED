package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unal.planta.servicio.ServicioSimulacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas HTTP de los endpoints de control de la simulación, con el servicio real.
 *
 * <p>El contexto de Spring, y con él el servicio, se comparte entre las pruebas. Por eso cada una
 * empieza reiniciando la simulación, que además la deja en pausa: así el reloj no avanza durante
 * la prueba.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ControladorSimulacion")
class ControladorSimulacionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void reiniciar() {
        servicio.reiniciar();
    }

    @Test
    @DisplayName("GET /api/simulacion devuelve tick, semilla maestra y ejecución")
    void simulacion() throws Exception {
        mvc.perform(get("/api/simulacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tick").value(0))
                .andExpect(jsonPath("$.semilla").value(7))
                .andExpect(jsonPath("$.enEjecucion").value(false));
    }

    @Test
    @DisplayName("POST paso avanza un tick y deja la simulación en pausa")
    void paso() throws Exception {
        mvc.perform(post("/api/simulacion/paso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tick").value(1))
                .andExpect(jsonPath("$.enEjecucion").value(false));
    }

    @Test
    @DisplayName("POST iniciar y pausar cambian si la simulación está en ejecución")
    void iniciarYPausar() throws Exception {
        mvc.perform(post("/api/simulacion/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enEjecucion").value(true));
        mvc.perform(post("/api/simulacion/pausar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enEjecucion").value(false));
    }

    @Test
    @DisplayName("POST reiniciar vuelve al tick 0")
    void reiniciarSimulacion() throws Exception {
        mvc.perform(post("/api/simulacion/paso"));
        mvc.perform(post("/api/simulacion/paso"));

        mvc.perform(post("/api/simulacion/reiniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tick").value(0))
                .andExpect(jsonPath("$.enEjecucion").value(false));
    }
}
