package edu.unal.planta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Pruebas HTTP de crear compañías y máquinas, y del catálogo de tipos.
 *
 * <p>Reiniciar la simulación <strong>conserva</strong> lo creado, así que estas pruebas dejan
 * compañías y máquinas en el servicio. Por eso la clase lleva {@link DirtiesContext}: al terminar,
 * Spring descarta el contexto y las demás clases de prueba arrancan con las 3 compañías iniciales.
 * Dentro de la clase, cada prueba usa nombres distintos y no depende de las otras.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@DisplayName("Crear compañías y máquinas")
class ControladorCreacionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("crear una compañía responde 201 y la agrega al final, con el id siguiente")
    void crearCompania() throws Exception {
        int total = cantidadDeCompanias();

        crearCompania("{\"nombre\": \"  Planta Oriente \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(total))
                .andExpect(jsonPath("$.nombre").value("Planta Oriente"))
                .andExpect(jsonPath("$.numeroMaquinas").value(6));

        mvc.perform(get("/api/companias"))
                .andExpect(jsonPath("$.length()").value(total + 1))
                .andExpect(jsonPath("$[" + total + "].nombre").value("Planta Oriente"));
    }

    @Test
    @DisplayName("un nombre repetido, aunque cambien las mayúsculas, responde 409")
    void companiaRepetida() throws Exception {
        crearCompania("{\"nombre\": \"PLANTA NORTE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una compañía llamada PLANTA NORTE"));
    }

    @Test
    @DisplayName("un nombre vacío o que falta responde 400")
    void companiaSinNombre() throws Exception {
        crearCompania("{\"nombre\": \"   \"}")
                .andExpect(status().isBadRequest());
        crearCompania("{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("crear una máquina responde 201 y la deja al final del inventario, en mayúsculas")
    void crearMaquina() throws Exception {
        crearMaquina(0, "{\"tipo\": \"torno\", \"etiqueta\": \"torno-02\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maquinas.length()").value(7))
                .andExpect(jsonPath("$.maquinas[6].indice").value(6))
                .andExpect(jsonPath("$.maquinas[6].etiqueta").value("TORNO-02"))
                .andExpect(jsonPath("$.maquinas[6].tipo").value("TORNO"))
                .andExpect(jsonPath("$.maquinas[6].estado").value("OPERANDO"));
    }

    @Test
    @DisplayName("una etiqueta repetida en la compañía responde 409; un tipo inexistente, 400")
    void maquinaInvalida() throws Exception {
        crearMaquina(1, "{\"tipo\": \"PRENSA\", \"etiqueta\": \"prensa-01\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "Ya existe una máquina con etiqueta PRENSA-01 en Planta Centro"));
        crearMaquina(1, "{\"tipo\": \"GRUA\", \"etiqueta\": \"GRUA-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El tipo 'GRUA' no existe: use PRENSA, TORNO, ROBOT, COMPRESOR, HORNO, BOMBA"));
        crearMaquina(99, "{\"tipo\": \"PRENSA\", \"etiqueta\": \"PRENSA-09\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tipos-maquina devuelve el catálogo con descripción y prefijo")
    void catalogo() throws Exception {
        mvc.perform(get("/api/tipos-maquina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[3].tipo").value("COMPRESOR"))
                .andExpect(jsonPath("$[3].descripcion").value("Compresor de tornillo"))
                .andExpect(jsonPath("$[3].prefijo").value("COMPRES"));
    }

    private int cantidadDeCompanias() throws Exception {
        String json = mvc.perform(get("/api/companias")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.length()");
    }

    private ResultActions crearCompania(String json) throws Exception {
        return mvc.perform(post("/api/companias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions crearMaquina(int compania, String json) throws Exception {
        return mvc.perform(post("/api/companias/" + compania + "/maquinas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
