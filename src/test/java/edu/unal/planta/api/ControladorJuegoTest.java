package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unal.planta.servicio.ServicioSimulacion;
import org.junit.jupiter.api.AfterEach;
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
 * Pruebas HTTP del modo manual y de la partida: el interruptor de modo, los botones de clasificar y
 * asignar, la economía en los DTOs y el resumen de la partida.
 *
 * <p>El contexto de Spring se comparte entre clases de prueba, y reiniciar conserva el modo. Por
 * eso cada prueba deja la simulación en automático al terminar, para no cambiar las demás.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Modo manual y partida")
class ControladorJuegoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void reiniciar() {
        servicio.reiniciar();
    }

    @AfterEach
    void volverAAutomatico() {
        servicio.cambiarModo("AUTOMATICO");
        servicio.reiniciar();
    }

    @Test
    @DisplayName("PUT modo cambia el modo; un modo que no existe responde 400")
    void cambiarModo() throws Exception {
        mvc.perform(get("/api/simulacion"))
                .andExpect(jsonPath("$.modo").value("AUTOMATICO"))
                .andExpect(jsonPath("$.duracionPartida").value(1000))
                .andExpect(jsonPath("$.partidaTerminada").value(false));

        modo("manual")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("MANUAL"));
        mvc.perform(get("/api/companias/1")).andExpect(jsonPath("$.modo").value("MANUAL"));

        modo("VOLAR").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("los botones del modo manual responden 409 en automático")
    void botonesEnAutomatico() throws Exception {
        mvc.perform(post("/api/companias/0/clasificar-siguiente"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La acción solo está disponible en modo manual"));
        asignar("MECANICO").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ciclo manual: inyectar, clasificar y asignar a una especialidad")
    void cicloManual() throws Exception {
        modo("MANUAL");
        mvc.perform(post("/api/companias/0/maquinas/0/inyectar-fallo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"indiceSensor\": 0, \"magnitud\": \"SEVERA\"}"));
        mvc.perform(post("/api/simulacion/paso"));

        mvc.perform(post("/api/companias/0/clasificar-siguiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos.length()").value(0))
                .andExpect(jsonPath("$.colaPrioridad.tamano").value(1));
        mvc.perform(post("/api/companias/0/clasificar-siguiente"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay eventos por clasificar en Planta Norte"));

        asignar("OTRO").andExpect(status().isBadRequest());
        asignar("mecanico")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinas[0].estado").value("EN_MANTENIMIENTO"));
        asignar("MECANICO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay órdenes por asignar en Planta Norte"));
    }

    @Test
    @DisplayName("la compañía trae su caja y el resumen de la partida trae los totales de cada una")
    void economiaYResumen() throws Exception {
        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.caja").value(300_000))
                .andExpect(jsonPath("$.enQuiebra").value(false));

        mvc.perform(post("/api/simulacion/paso"));

        mvc.perform(get("/api/partida/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tick").value(1))
                .andExpect(jsonPath("$.terminada").value(false))
                .andExpect(jsonPath("$.companias.length()").value(3))
                .andExpect(jsonPath("$.companias[0].ingresos").isNumber())
                .andExpect(jsonPath("$.companias[0].disponibilidad").value(1.0))
                .andExpect(jsonPath("$.companias[0].enQuiebra").value(false));
    }

    private ResultActions modo(String modo) throws Exception {
        return mvc.perform(put("/api/simulacion/modo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modo\": \"" + modo + "\"}"));
    }

    private ResultActions asignar(String especialidad) throws Exception {
        return mvc.perform(post("/api/companias/0/asignar-siguiente")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"especialidad\": \"" + especialidad + "\"}"));
    }
}
