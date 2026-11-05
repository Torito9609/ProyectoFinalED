package edu.unal.planta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de humo del arranque de Spring.
 *
 * <p>Si la configuración del proyecto tuviera un error (por ejemplo, un componente que no se
 * puede crear), el contexto no cargaría y esta prueba fallaría antes de llegar a cualquier
 * endpoint.</p>
 */
@SpringBootTest
@DisplayName("AplicacionPlanta")
class AplicacionPlantaTest {

    @Test
    @DisplayName("el contexto de Spring carga sin errores")
    void contextoCarga() {
        // El cuerpo vacío es intencional: @SpringBootTest ya falla si el contexto no arranca.
    }
}
