package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de CORS: el frontend local puede llamar a la API y un origen externo no.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ConfiguracionCors")
class ConfiguracionCorsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("un origen local recibe el permiso de CORS")
    void origenLocal() throws Exception {
        mvc.perform(get("/api/simulacion").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"));
    }

    @Test
    @DisplayName("un origen externo es rechazado")
    void origenExterno() throws Exception {
        mvc.perform(get("/api/simulacion").header(HttpHeaders.ORIGIN, "http://ejemplo.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
