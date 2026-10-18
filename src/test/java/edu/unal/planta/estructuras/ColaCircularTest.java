package edu.unal.planta.estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link ColaCircular}.
 *
 * <p>Cubre los casos obligatorios definidos en el documento de diseño: cola vacía, llenado exacto,
 * sobrescritura, varias vueltas completas, desencolar después de dar la vuelta y capacidad 1.</p>
 *
 * <p>Varias pruebas verifican también los índices físicos de frente y final, porque son los datos
 * que la vista dibuja: si estuvieran mal, el panel mostraría un comportamiento que no corresponde
 * al de la estructura.</p>
 */
@DisplayName("ColaCircular")
class ColaCircularTest {

    @Nested
    @DisplayName("Construcción")
    class Construccion {

        @Test
        @DisplayName("rechaza una capacidad de cero o negativa")
        void rechazaCapacidadInvalida() {
            assertThrows(IllegalArgumentException.class, () -> new ColaCircular<String>(0));
            assertThrows(IllegalArgumentException.class, () -> new ColaCircular<String>(-5));
        }

        @Test
        @DisplayName("una cola nueva conserva la capacidad pedida")
        void conservaLaCapacidad() {
            assertEquals(12, new ColaCircular<String>(12).capacidad());
        }
    }

    @Nested
    @DisplayName("Cola vacía")
    class ColaVacia {

        private final ColaCircular<String> cola = new ColaCircular<>(3);

        @Test
        @DisplayName("no tiene elementos y no está llena")
        void estadoInicial() {
            assertTrue(cola.estaVacia());
            assertFalse(cola.estaLlena());
            assertEquals(0, cola.tamano());
            assertEquals(0, cola.sobrescrituras());
            assertTrue(cola.elementosEnOrden().isEmpty());
        }

        @Test
        @DisplayName("todas sus celdas están vacías")
        void celdasVacias() {
            for (int i = 0; i < cola.capacidad(); i++) {
                assertTrue(cola.contenidoCelda(i).isEmpty(), "la celda " + i + " debería estar vacía");
            }
        }

        @Test
        @DisplayName("desencolar, ver el frente y ver el más reciente lanzan excepción")
        void operacionesDeLecturaFallan() {
            assertThrows(NoSuchElementException.class, cola::desencolar);
            assertThrows(NoSuchElementException.class, cola::verFrente);
            assertThrows(NoSuchElementException.class, cola::verMasReciente);
        }

        @Test
        @DisplayName("el frente y el final apuntan a la misma celda")
        void punterosCoinciden() {
            assertEquals(cola.indiceFrente(), cola.indiceFinal());
        }
    }

    @Nested
    @DisplayName("Llenado exacto")
    class LlenadoExacto {

        @Test
        @DisplayName("al llenarse sin excederse no hay ninguna sobrescritura")
        void sinSobrescrituras() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c");

            assertTrue(cola.estaLlena());
            assertFalse(cola.estaVacia());
            assertEquals(3, cola.tamano());
            assertEquals(0, cola.sobrescrituras());
            assertEquals(List.of("a", "b", "c"), cola.elementosEnOrden());
        }

        @Test
        @DisplayName("los elementos quedan en celdas consecutivas desde la cero")
        void ocupaCeldasConsecutivas() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b");

            assertEquals("a", cola.contenidoCelda(0).orElseThrow());
            assertEquals("b", cola.contenidoCelda(1).orElseThrow());
            assertTrue(cola.contenidoCelda(2).isEmpty());
        }

        @Test
        @DisplayName("con la cola llena el final vuelve a coincidir con el frente")
        void finalVuelveAlFrente() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c");

            assertEquals(0, cola.indiceFrente());
            assertEquals(0, cola.indiceFinal());
        }

        @Test
        @DisplayName("el frente es el más antiguo y el más reciente es el último encolado")
        void extremosCorrectos() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c");

            assertEquals("a", cola.verFrente());
            assertEquals("c", cola.verMasReciente());
        }
    }

    @Nested
    @DisplayName("Sobrescritura")
    class Sobrescritura {

        @Test
        @DisplayName("al encolar en una cola llena se descarta el elemento más antiguo")
        void descartaElMasAntiguo() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c", "d");

            assertEquals(3, cola.tamano(), "el tamaño no cambia: entró uno y salió uno");
            assertEquals(1, cola.sobrescrituras());
            assertEquals(List.of("b", "c", "d"), cola.elementosEnOrden());
            assertEquals("b", cola.verFrente());
            assertEquals("d", cola.verMasReciente());
        }

        @Test
        @DisplayName("el nuevo elemento ocupa la celda física del que descartó")
        void reutilizaLaCelda() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c", "d");

            assertEquals("d", cola.contenidoCelda(0).orElseThrow(),
                    "la celda 0 tenía la 'a' y ahora debe tener la 'd'");
            assertEquals(1, cola.indiceFrente(), "el frente avanzó a la 'b'");
            assertEquals(1, cola.indiceFinal(), "con la cola llena el final sigue al frente");
        }
    }

    @Nested
    @DisplayName("Varias vueltas completas")
    class VariasVueltas {

        @Test
        @DisplayName("tras diez elementos en una cola de tres solo quedan los tres últimos")
        void conservaSoloLosUltimos() {
            ColaCircular<Integer> cola = new ColaCircular<>(3);
            for (int i = 1; i <= 10; i++) {
                cola.encolar(i);
            }

            assertEquals(3, cola.tamano());
            assertEquals(7, cola.sobrescrituras(), "diez encolados en capacidad tres descartan siete");
            assertEquals(List.of(8, 9, 10), cola.elementosEnOrden());
        }

        @Test
        @DisplayName("el frente da la vuelta y vuelve a pasar por la celda cero")
        void elFrenteDaLaVuelta() {
            ColaCircular<Integer> cola = new ColaCircular<>(3);
            for (int i = 1; i <= 9; i++) {
                cola.encolar(i);
            }

            assertEquals(0, cola.indiceFrente(), "tras seis sobrescrituras el frente volvió a cero");
            assertEquals(List.of(7, 8, 9), cola.elementosEnOrden());
        }

        @Test
        @DisplayName("la capacidad nunca cambia, por muchos elementos que pasen")
        void capacidadFija() {
            ColaCircular<Integer> cola = new ColaCircular<>(4);
            for (int i = 0; i < 1000; i++) {
                cola.encolar(i);
            }

            assertEquals(4, cola.capacidad());
            assertEquals(4, cola.tamano());
            assertEquals(996, cola.sobrescrituras());
        }
    }

    @Nested
    @DisplayName("Desencolar después de dar la vuelta")
    class DesencolarTrasLaVuelta {

        @Test
        @DisplayName("los elementos salen en orden de antigüedad aunque el arreglo esté rotado")
        void saleEnOrdenDeAntiguedad() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c", "d", "e");
            // Contenido lógico: c, d, e. Contenido físico: celda 0 = d, celda 1 = e, celda 2 = c.

            assertEquals("c", cola.desencolar());
            assertEquals("d", cola.desencolar());
            assertEquals("e", cola.desencolar());
            assertTrue(cola.estaVacia());
        }

        @Test
        @DisplayName("al desencolar se libera la celda y el frente avanza")
        void liberaLaCelda() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c", "d");
            // Frente en la celda 1, con la "b".

            assertEquals("b", cola.desencolar());
            assertTrue(cola.contenidoCelda(1).isEmpty(), "la celda liberada queda vacía");
            assertEquals(2, cola.indiceFrente());
            assertEquals(2, cola.tamano());
            assertEquals(List.of("c", "d"), cola.elementosEnOrden());
        }

        @Test
        @DisplayName("se puede volver a encolar en las celdas liberadas")
        void vuelveAEncolar() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c", "d");
            cola.desencolar();
            cola.desencolar();
            cola.encolar("e");

            assertEquals(2, cola.tamano());
            assertEquals(List.of("d", "e"), cola.elementosEnOrden());
            assertEquals(1, cola.sobrescrituras(), "encolar en una cola con espacio no sobrescribe");
        }
    }

    @Nested
    @DisplayName("Capacidad 1")
    class CapacidadUno {

        @Test
        @DisplayName("cada elemento nuevo reemplaza al anterior")
        void cadaUnoReemplazaAlAnterior() {
            ColaCircular<String> cola = new ColaCircular<>(1);

            cola.encolar("a");
            assertTrue(cola.estaLlena());
            assertEquals("a", cola.verFrente());
            assertEquals(0, cola.sobrescrituras());

            cola.encolar("b");
            assertEquals(1, cola.tamano());
            assertEquals(1, cola.sobrescrituras());
            assertEquals("b", cola.verFrente());
            assertEquals("b", cola.verMasReciente());
            assertEquals(List.of("b"), cola.elementosEnOrden());
        }

        @Test
        @DisplayName("el frente y el final siempre apuntan a la única celda")
        void punterosSiempreEnCero() {
            ColaCircular<String> cola = new ColaCircular<>(1);
            cola.encolar("a");
            cola.encolar("b");

            assertEquals(0, cola.indiceFrente());
            assertEquals(0, cola.indiceFinal());
        }

        @Test
        @DisplayName("se puede desencolar y queda vacía")
        void desencolarLaDejaVacia() {
            ColaCircular<String> cola = new ColaCircular<>(1);
            cola.encolar("a");

            assertEquals("a", cola.desencolar());
            assertTrue(cola.estaVacia());
            assertThrows(NoSuchElementException.class, cola::desencolar);
        }
    }

    @Nested
    @DisplayName("Acceso de solo lectura para la vista")
    class AccesoDeSoloLectura {

        @Test
        @DisplayName("la lista de elementos no se puede modificar")
        void listaNoModificable() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b");
            List<String> elementos = cola.elementosEnOrden();

            assertThrows(UnsupportedOperationException.class, () -> elementos.add("c"));
        }

        @Test
        @DisplayName("la lista es una copia: no refleja los cambios posteriores de la cola")
        void listaEsUnaCopia() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b");
            List<String> antes = cola.elementosEnOrden();

            cola.encolar("c");

            assertEquals(List.of("a", "b"), antes);
            assertEquals(List.of("a", "b", "c"), cola.elementosEnOrden());
        }

        @Test
        @DisplayName("consultar una celda fuera del arreglo lanza excepción")
        void celdaFueraDeRango() {
            ColaCircular<String> cola = new ColaCircular<>(3);

            assertThrows(IndexOutOfBoundsException.class, () -> cola.contenidoCelda(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> cola.contenidoCelda(3));
        }
    }

    @Nested
    @DisplayName("Otras reglas")
    class OtrasReglas {

        @Test
        @DisplayName("no acepta elementos nulos, porque una celda nula significa celda vacía")
        void rechazaNulos() {
            ColaCircular<String> cola = new ColaCircular<>(3);

            assertThrows(NullPointerException.class, () -> cola.encolar(null));
        }

        @Test
        @DisplayName("vaciar deja la cola como recién creada")
        void vaciarReinicia() {
            ColaCircular<String> cola = encolar(new ColaCircular<>(3), "a", "b", "c", "d");

            cola.vaciar();

            assertTrue(cola.estaVacia());
            assertEquals(0, cola.tamano());
            assertEquals(0, cola.sobrescrituras(), "reiniciar la simulación también reinicia el contador");
            assertEquals(0, cola.indiceFrente());
            assertEquals(0, cola.indiceFinal());
            assertTrue(cola.contenidoCelda(0).isEmpty());
        }
    }

    /**
     * Encola varios elementos de corrido, para que las pruebas no repitan llamadas.
     *
     * @param cola      cola destino
     * @param elementos elementos a encolar, en ese orden
     * @return la misma cola recibida, ya con los elementos
     */
    private static ColaCircular<String> encolar(ColaCircular<String> cola, String... elementos) {
        for (String elemento : elementos) {
            cola.encolar(elemento);
        }
        return cola;
    }
}
