package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unal.planta.api.dto.MaquinaDto;
import edu.unal.planta.modelo.ConfiguracionPlanta;
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
 * Pruebas HTTP del overhaul y del reemplazo: aprobar, ver el montículo, despachar el camión y
 * reemplazar una máquina.
 *
 * <p>Para que una máquina tenga recomendación sin depender del azar, se usa el modo manual: se le
 * inyectan fallos críticos y se reparan a mano hasta que su puntaje llega al umbral. Como en
 * {@code ControladorJuegoTest}, cada prueba deja la simulación en automático al terminar.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Overhaul y reemplazo")
class ControladorOverhaulTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServicioSimulacion servicio;

    @BeforeEach
    void reiniciar() {
        servicio.cambiarModo("MANUAL");
        servicio.reiniciar();
    }

    @AfterEach
    void volverAAutomatico() {
        servicio.cambiarModo("AUTOMATICO");
        servicio.reiniciar();
    }

    @Test
    @DisplayName("GET overhauls: el montículo vacío y los técnicos del fabricante libres")
    void monticuloVacio() throws Exception {
        mvc.perform(get("/api/overhauls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cima").isEmpty())
                .andExpect(jsonPath("$.ordenDeSalida.length()").value(0))
                .andExpect(jsonPath("$.tecnicos.length()").value(2))
                .andExpect(jsonPath("$.tecnicosLibres.length()").value(2))
                .andExpect(jsonPath("$.duracionOverhaul").value(ConfiguracionPlanta.DURACION_OVERHAUL));
    }

    @Test
    @DisplayName("aprobar: 400 sin datos, 404 si no existe, 409 sin recomendación o repetido")
    void aprobarOverhaul() throws Exception {
        aprobar("{}").andExpect(status().isBadRequest());
        aprobar("{\"idCompania\": 9, \"indiceMaquina\": 0}").andExpect(status().isNotFound());
        aprobar("{\"idCompania\": 0, \"indiceMaquina\": 99}").andExpect(status().isNotFound());
        aprobar("{\"idCompania\": 0, \"indiceMaquina\": 0}").andExpect(status().isConflict());

        int puntaje = llevarAlUmbral(0);
        aprobar("{\"idCompania\": 0, \"indiceMaquina\": 0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cima.etiqueta").value("PRENSA-01"))
                .andExpect(jsonPath("$.cima.nombreCompania").value("Planta Norte"))
                .andExpect(jsonPath("$.cima.puntajeCongelado").value(puntaje))
                .andExpect(jsonPath("$.ordenDeSalida.length()").value(1));
        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.maquinas[0].overhaulPendiente").value(true));

        aprobar("{\"idCompania\": 0, \"indiceMaquina\": 0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("PRENSA-01 ya tiene un overhaul pendiente"));
    }

    @Test
    @DisplayName("despachar el camión: 409 en automático o sin solicitudes; en manual la máquina entra en overhaul")
    void asignarSiguienteOverhaul() throws Exception {
        mvc.perform(post("/api/overhauls/asignar-siguiente"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El camión espera: no hay solicitudes de overhaul"));

        llevarAlUmbral(0);
        aprobar("{\"idCompania\": 0, \"indiceMaquina\": 0}");
        servicio.cambiarModo("AUTOMATICO");
        mvc.perform(post("/api/overhauls/asignar-siguiente")).andExpect(status().isConflict());
        servicio.cambiarModo("MANUAL");

        mvc.perform(post("/api/overhauls/asignar-siguiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cima").isEmpty())
                .andExpect(jsonPath("$.tecnicosLibres.length()").value(1))
                .andExpect(jsonPath("$.tecnicos[0].libre").value(false))
                .andExpect(jsonPath("$.tecnicos[0].solicitudActual.etiqueta").value("PRENSA-01"))
                .andExpect(jsonPath("$.tecnicos[0].ticksRestantes").value(ConfiguracionPlanta.DURACION_OVERHAUL));
        mvc.perform(get("/api/companias/0"))
                .andExpect(jsonPath("$.maquinas[0].estado").value("EN_OVERHAUL"))
                .andExpect(jsonPath("$.maquinas[0].overhaulPendiente").value(false));
    }

    @Test
    @DisplayName("reemplazo: 409 sin recomendación de reemplazo; con ella, máquina nueva en el mismo índice")
    void reemplazarMaquina() throws Exception {
        mvc.perform(post("/api/companias/0/maquinas/0/reemplazo")).andExpect(status().isConflict());
        mvc.perform(post("/api/companias/0/maquinas/99/reemplazo")).andExpect(status().isNotFound());

        llevarAlUmbral(0);
        aprobar("{\"idCompania\": 0, \"indiceMaquina\": 0}");
        servicio.asignarSiguienteOverhaul();
        while (maquina(0).estado().equals("EN_OVERHAUL")) {
            servicio.paso();
        }
        llevarAlUmbral(0);
        long caja = servicio.estadoCompania(0).caja();

        mvc.perform(post("/api/companias/0/maquinas/0/reemplazo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinas[0].etiqueta").value("PRENSA-01"))
                .andExpect(jsonPath("$.maquinas[0].puntajeFallos").value(0))
                .andExpect(jsonPath("$.maquinas[0].numeroOverhauls").value(0))
                .andExpect(jsonPath("$.maquinas[0].recomendacion").value("NINGUNA"))
                .andExpect(jsonPath("$.caja").value(caja - ConfiguracionPlanta.COSTO_REEMPLAZO));
    }

    private ResultActions aprobar(String cuerpo) throws Exception {
        return mvc.perform(post("/api/overhauls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private MaquinaDto maquina(int indice) {
        return servicio.estadoCompania(0).maquinas().get(indice);
    }

    /**
     * Lleva una máquina de la compañía 0 hasta el umbral de overhaul con fallos críticos inyectados
     * y reparados a mano. Al terminar, la máquina queda operando y libre.
     *
     * @return el puntaje de la máquina
     */
    private int llevarAlUmbral(int indice) {
        String especialidad = maquina(indice).sensores().get(0).tipoFallo();
        while (maquina(indice).puntajeFallos() < ConfiguracionPlanta.UMBRAL_OVERHAUL) {
            servicio.inyectarFallo(0, indice, 0, "SEVERA");
            servicio.paso();
            servicio.clasificarSiguiente(0);
            servicio.asignarSiguiente(0, especialidad);
            while (!maquina(indice).estado().equals("OPERANDO")) {
                servicio.paso();
            }
        }
        return maquina(indice).puntajeFallos();
    }
}
