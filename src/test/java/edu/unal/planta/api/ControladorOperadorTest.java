package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unal.planta.servicio.ServicioSimulacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Pruebas HTTP de las acciones del operador y de los errores 400, 404 y 409.
 *
 * <p>Cada prueba empieza reiniciando la simulación: en el tick 0 todas las máquinas están
 * operando, sin fallos y con la pila de deshacer vacía, así que el resultado no depende del
 * azar.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ControladorOperador")
class ControladorOperadorTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void reiniciar() {
        servicio.reiniciar();
    }

    @Test
    @DisplayName("inyectar un fallo deja la anomalía en la máquina")
    void inyectarFallo() throws Exception {
        inyectar(0, "{\"indiceSensor\": 0, \"magnitud\": \"SEVERA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinas[0].tieneAnomalia").value(true));
    }

    @Test
    @DisplayName("inyectar dos veces en la misma máquina responde 409 y queda en la bitácora")
    void inyectarDosVeces() throws Exception {
        inyectar(0, "{\"indiceSensor\": 0, \"magnitud\": \"LEVE\"}");

        inyectar(0, "{\"indiceSensor\": 1, \"magnitud\": \"SEVERA\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede inyectar un fallo en PRENSA-01: ya tiene un fallo activo"));
        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.bitacora[-1]").value(
                        "Tick 0: Inyección rechazada: PRENSA-01 ya tiene un fallo activo"));
    }

    @Test
    @DisplayName("una máquina o un sensor que no existen responden 404")
    void noEncontrado() throws Exception {
        mvc.perform(post("/api/companias/0/maquinas/99/parar-o-arrancar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe una máquina con índice 99"));
        inyectar(0, "{\"indiceSensor\": 9, \"magnitud\": \"LEVE\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("una magnitud que no existe o una carga fuera de rango responden 400")
    void entradaInvalida() throws Exception {
        inyectar(0, "{\"indiceSensor\": 0, \"magnitud\": \"ENORME\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/companias/0/maquinas/0/carga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carga\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La carga debe estar entre 0 y 100: 150"));
    }

    @Test
    @DisplayName("cambiar la carga la apila en la pila de deshacer")
    void cambiarCarga() throws Exception {
        mvc.perform(put("/api/companias/0/maquinas/1/carga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carga\": 35}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinas[1].carga").value(35))
                .andExpect(jsonPath("$.pilaDeshacer.length()").value(1));
    }

    @Test
    @DisplayName("parar y deshacer devuelve la máquina a operando")
    void pararYDeshacer() throws Exception {
        mvc.perform(post("/api/companias/0/maquinas/2/parar-o-arrancar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinas[2].estado").value("PARADA_MANUAL"))
                .andExpect(jsonPath("$.pilaDeshacer[0]").value("Parar ROBOT-01"));

        mvc.perform(post("/api/companias/0/deshacer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinas[2].estado").value("OPERANDO"))
                .andExpect(jsonPath("$.pilaDeshacer.length()").value(0));
    }

    @Test
    @DisplayName("deshacer con la pila vacía responde 409")
    void deshacerVacia() throws Exception {
        mvc.perform(post("/api/companias/0/deshacer"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay acciones para deshacer"));
    }

    @Test
    @DisplayName("una acción en una compañía no toca a las demás")
    void companiasIndependientes() throws Exception {
        mvc.perform(post("/api/companias/1/maquinas/0/parar-o-arrancar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCompania").value(1))
                .andExpect(jsonPath("$.maquinas[0].estado").value("PARADA_MANUAL"));

        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.maquinas[0].estado").value("OPERANDO"))
                .andExpect(jsonPath("$.pilaDeshacer.length()").value(0));
    }

    private ResultActions inyectar(int maquina, String json) throws Exception {
        return mvc.perform(post("/api/companias/0/maquinas/" + maquina + "/inyectar-fallo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
