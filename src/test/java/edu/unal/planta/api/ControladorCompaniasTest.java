package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Pruebas HTTP de la consulta de las compañías, con el servicio real.
 *
 * <p>Cada prueba empieza reiniciando la simulación, que la deja en pausa en el tick 0.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ControladorCompanias")
class ControladorCompaniasTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void reiniciar() {
        servicio.reiniciar();
    }

    @Test
    @DisplayName("GET /api/companias devuelve las 3 compañías con sus totales")
    void companias() throws Exception {
        mvc.perform(get("/api/companias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(0))
                .andExpect(jsonPath("$[0].nombre").value("Planta Norte"))
                .andExpect(jsonPath("$[1].nombre").value("Planta Centro"))
                .andExpect(jsonPath("$[2].nombre").value("Planta Sur"))
                .andExpect(jsonPath("$[0].numeroMaquinas").value(6))
                .andExpect(jsonPath("$[0].maquinasPorEstado.OPERANDO").value(6))
                .andExpect(jsonPath("$[0].maquinasPorEstado.EN_ALARMA").value(0))
                .andExpect(jsonPath("$[0].ordenesPendientes").value(0))
                .andExpect(jsonPath("$[0].tecnicosLibres").value(2));
    }

    @Test
    @DisplayName("GET /api/companias/{id} devuelve las 6 máquinas, los 4 carriles y la cola circular")
    void compania() throws Exception {
        mvc.perform(get("/api/companias/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCompania").value(2))
                .andExpect(jsonPath("$.nombre").value("Planta Sur"))
                .andExpect(jsonPath("$.tick").value(0))
                .andExpect(jsonPath("$.maquinas.length()").value(6))
                .andExpect(jsonPath("$.maquinas[0].etiqueta").value("PRENSA-01"))
                .andExpect(jsonPath("$.maquinas[0].lecturas.celdas.length()").value(12))
                .andExpect(jsonPath("$.colaPrioridad.carriles.length()").value(4));
    }

    @Test
    @DisplayName("el tipo de máquina viaja como código del catálogo y como descripción")
    void tipoDeMaquina() throws Exception {
        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.maquinas[0].tipo").value("PRENSA"))
                .andExpect(jsonPath("$.maquinas[0].tipoDescripcion").value("Prensa hidráulica"));
    }

    @Test
    @DisplayName("una compañía que no existe responde 404")
    void companiaInexistente() throws Exception {
        mvc.perform(get("/api/companias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe una compañía con id 99"));
    }
}
