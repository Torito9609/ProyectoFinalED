package edu.unal.planta.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unal.planta.api.dto.HistorialDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas del historial por ventana de ticks: el total y el pico que da el árbol de segmentos deben
 * coincidir con sumar y buscar el máximo en la serie, tick por tick.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Historial")
class ControladorHistorialTest {

    private static final int TICKS = 200;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void avanzar() {
        servicio.reiniciar();
        for (int i = 0; i < TICKS; i++) {
            servicio.paso();
        }
    }

    @AfterEach
    void reiniciar() {
        servicio.reiniciar();
    }

    @Test
    @DisplayName("sin ventana va del tick 0 al último registrado, y el total y el pico coinciden con la serie")
    void historialGlobal() throws Exception {
        mvc.perform(get("/api/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcance").value("Global"))
                .andExpect(jsonPath("$.desde").value(0))
                .andExpect(jsonPath("$.hasta").value(TICKS - 1))
                .andExpect(jsonPath("$.fallosPorTick.length()").value(TICKS))
                .andExpect(jsonPath("$.costoPorTick.length()").value(TICKS));

        HistorialDto global = servicio.historial(null, 50, 149);
        verificarContraLaSerie(global);
        long sumaCompanias = 0;
        for (int id = 0; id < 3; id++) {
            sumaCompanias += servicio.historial(id, 50, 149).totalFallos();
        }
        assertEquals(sumaCompanias, global.totalFallos(), "el global suma las compañías");
    }

    @Test
    @DisplayName("la ventana de una compañía trae su nombre y un valor por tick")
    void historialDeCompania() throws Exception {
        mvc.perform(get("/api/historial").param("compania", "1").param("desde", "10").param("hasta", "19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcance").value("Planta Centro"))
                .andExpect(jsonPath("$.fallosPorTick.length()").value(10));

        verificarContraLaSerie(servicio.historial(1, 0, TICKS - 1));
    }

    @Test
    @DisplayName("una ventana inválida responde 400 y una compañía que no existe, 404")
    void errores() throws Exception {
        mvc.perform(get("/api/historial").param("desde", "5").param("hasta", "2"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/historial").param("desde", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/historial").param("hasta", "1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/historial").param("compania", "9")).andExpect(status().isNotFound());
    }

    private static void verificarContraLaSerie(HistorialDto historial) {
        assertEquals(historial.fallosPorTick().stream().mapToLong(Integer::longValue).sum(),
                historial.totalFallos());
        assertEquals(Collections.max(historial.fallosPorTick()), historial.picoFallos());
        assertEquals(historial.costoPorTick().stream().mapToLong(Integer::longValue).sum(),
                historial.totalCosto());
        assertEquals(Collections.max(historial.costoPorTick()), historial.picoCosto());
    }
}
