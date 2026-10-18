package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link ColaPrioridadOrdenes}.
 *
 * <p>Cubre los casos obligatorios definidos en el documento de diseño: orden por prioridad, FIFO
 * dentro de un mismo nivel, una P1 insertada de última que sale primero, y extraer con la
 * estructura vacía.</p>
 *
 * <p>La prueba de FIFO dentro del nivel es la más importante de todas: es justamente la garantía
 * que {@code PriorityQueue} no ofrece y la razón por la que esta clase existe.</p>
 */
@DisplayName("ColaPrioridadOrdenes")
class ColaPrioridadOrdenesTest {

    private final ColaPrioridadOrdenes cola = new ColaPrioridadOrdenes();

    @Nested
    @DisplayName("Cola vacía")
    class ColaVacia {

        @Test
        @DisplayName("extraer no lanza excepción, devuelve un Optional vacío")
        void extraerDevuelveVacio() {
            assertEquals(Optional.empty(), cola.extraer());
        }

        @Test
        @DisplayName("ver el siguiente y su nivel también devuelven vacío")
        void consultasDevuelvenVacio() {
            assertEquals(Optional.empty(), cola.verSiguiente());
            assertEquals(Optional.empty(), cola.nivelDelSiguiente());
        }

        @Test
        @DisplayName("está vacía y todos los niveles están en cero")
        void todosLosNivelesEnCero() {
            assertTrue(cola.estaVacia());
            assertEquals(0, cola.tamano());
            for (Prioridad prioridad : Prioridad.values()) {
                assertEquals(0, cola.tamanoNivel(prioridad));
                assertTrue(cola.ordenesDe(prioridad).isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Orden de extracción")
    class OrdenDeExtraccion {

        @Test
        @DisplayName("sale primero la de mayor prioridad, sin importar el orden de inserción")
        void saleLaDeMayorPrioridad() {
            cola.insertar(orden(1, Prioridad.P3));
            cola.insertar(orden(2, Prioridad.P1));
            cola.insertar(orden(3, Prioridad.P4));
            cola.insertar(orden(4, Prioridad.P2));

            assertEquals(List.of(2, 4, 1, 3), extraerTodas());
        }

        @Test
        @DisplayName("una P1 insertada de última sale antes que todas las anteriores")
        void laP1SeAdelanta() {
            cola.insertar(orden(1, Prioridad.P4));
            cola.insertar(orden(2, Prioridad.P3));
            cola.insertar(orden(3, Prioridad.P2));
            cola.insertar(orden(4, Prioridad.P1));

            assertEquals(4, cola.extraer().orElseThrow().id(),
                    "la P1 llegó de última pero debe atenderse primero");
            assertEquals(List.of(3, 2, 1), extraerTodas());
        }

        @Test
        @DisplayName("dentro de un mismo nivel se respeta el orden de llegada")
        void fifoDentroDelNivel() {
            cola.insertar(orden(1, Prioridad.P2));
            cola.insertar(orden(2, Prioridad.P2));
            cola.insertar(orden(3, Prioridad.P2));

            assertEquals(List.of(1, 2, 3), extraerTodas(),
                    "cada nivel es una cola FIFO, así que el desempate es por orden de llegada");
        }

        @Test
        @DisplayName("el orden de llegada se respeta en todos los niveles a la vez")
        void fifoEnTodosLosNiveles() {
            cola.insertar(orden(1, Prioridad.P2));
            cola.insertar(orden(2, Prioridad.P1));
            cola.insertar(orden(3, Prioridad.P2));
            cola.insertar(orden(4, Prioridad.P1));
            cola.insertar(orden(5, Prioridad.P2));

            assertEquals(List.of(2, 4, 1, 3, 5), extraerTodas());
        }

        @Test
        @DisplayName("la cola queda vacía después de extraer todo")
        void quedaVaciaAlFinal() {
            cola.insertar(orden(1, Prioridad.P1));
            cola.insertar(orden(2, Prioridad.P3));

            extraerTodas();

            assertTrue(cola.estaVacia());
            assertEquals(Optional.empty(), cola.extraer());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("ver el siguiente no lo saca de la cola")
        void verSiguienteNoExtrae() {
            cola.insertar(orden(1, Prioridad.P3));
            cola.insertar(orden(2, Prioridad.P1));

            assertEquals(2, cola.verSiguiente().orElseThrow().id());
            assertEquals(2, cola.verSiguiente().orElseThrow().id(), "consultar dos veces da lo mismo");
            assertEquals(2, cola.tamano(), "el tamaño no cambió");
        }

        @Test
        @DisplayName("informa el nivel del que saldrá la próxima orden")
        void informaElNivelActivo() {
            cola.insertar(orden(1, Prioridad.P4));
            assertEquals(Optional.of(Prioridad.P4), cola.nivelDelSiguiente());

            cola.insertar(orden(2, Prioridad.P2));
            assertEquals(Optional.of(Prioridad.P2), cola.nivelDelSiguiente(),
                    "al llegar una P2 el carril activo pasa a ser el de P2");
        }

        @Test
        @DisplayName("cuenta las órdenes en total y por nivel")
        void cuentaPorNivel() {
            cola.insertar(orden(1, Prioridad.P1));
            cola.insertar(orden(2, Prioridad.P3));
            cola.insertar(orden(3, Prioridad.P3));

            assertEquals(3, cola.tamano());
            assertEquals(1, cola.tamanoNivel(Prioridad.P1));
            assertEquals(0, cola.tamanoNivel(Prioridad.P2));
            assertEquals(2, cola.tamanoNivel(Prioridad.P3));
            assertEquals(0, cola.tamanoNivel(Prioridad.P4));
            assertFalse(cola.estaVacia());
        }

        @Test
        @DisplayName("las órdenes de un nivel se listan en orden FIFO")
        void listaElNivelEnOrden() {
            cola.insertar(orden(1, Prioridad.P2));
            cola.insertar(orden(2, Prioridad.P2));

            List<Integer> ids = cola.ordenesDe(Prioridad.P2).stream().map(OrdenTrabajo::id).toList();

            assertEquals(List.of(1, 2), ids);
        }

        @Test
        @DisplayName("la lista de un nivel no se puede modificar")
        void listaNoModificable() {
            cola.insertar(orden(1, Prioridad.P2));
            List<OrdenTrabajo> ordenes = cola.ordenesDe(Prioridad.P2);
            OrdenTrabajo otra = orden(2, Prioridad.P2);

            assertThrows(UnsupportedOperationException.class, () -> ordenes.add(otra));
        }

        @Test
        @DisplayName("la lista de un nivel es una copia y no refleja cambios posteriores")
        void listaEsUnaCopia() {
            cola.insertar(orden(1, Prioridad.P2));
            List<OrdenTrabajo> antes = cola.ordenesDe(Prioridad.P2);

            cola.insertar(orden(2, Prioridad.P2));

            assertEquals(1, antes.size());
            assertEquals(2, cola.ordenesDe(Prioridad.P2).size());
        }
    }

    @Nested
    @DisplayName("Otras reglas")
    class OtrasReglas {

        @Test
        @DisplayName("no acepta órdenes nulas")
        void rechazaNulos() {
            assertThrows(NullPointerException.class, () -> cola.insertar(null));
            assertThrows(NullPointerException.class, () -> cola.tamanoNivel(null));
            assertThrows(NullPointerException.class, () -> cola.ordenesDe(null));
        }

        @Test
        @DisplayName("vaciar elimina las órdenes de los cuatro niveles")
        void vaciarLimpiaTodo() {
            for (Prioridad prioridad : Prioridad.values()) {
                cola.insertar(orden(prioridad.nivel() + 1, prioridad));
            }

            cola.vaciar();

            assertTrue(cola.estaVacia());
            assertEquals(0, cola.tamano());
        }
    }

    @Nested
    @DisplayName("Contrato del enum Prioridad")
    class ContratoDelEnum {

        @Test
        @DisplayName("el nivel coincide con la posición, porque es el índice del arreglo de colas")
        void nivelCoincideConLaPosicion() {
            assertEquals(4, Prioridad.NIVELES);
            for (Prioridad prioridad : Prioridad.values()) {
                assertEquals(prioridad.ordinal(), prioridad.nivel());
                assertEquals(prioridad, Prioridad.porNivel(prioridad.nivel()));
            }
        }

        @Test
        @DisplayName("las prioridades están declaradas de la más urgente a la menos urgente")
        void declaradasEnOrdenDeUrgencia() {
            assertEquals(
                    List.of(Prioridad.P1, Prioridad.P2, Prioridad.P3, Prioridad.P4),
                    List.of(Prioridad.values()));
        }

        @Test
        @DisplayName("un nivel fuera de rango se rechaza")
        void nivelFueraDeRango() {
            assertThrows(IllegalArgumentException.class, () -> Prioridad.porNivel(-1));
            assertThrows(IllegalArgumentException.class, () -> Prioridad.porNivel(4));
        }
    }

    /**
     * Extrae todas las órdenes y devuelve sus identificadores en el orden en que salieron.
     *
     * @return lista de identificadores en orden de extracción
     */
    private List<Integer> extraerTodas() {
        List<Integer> ids = new ArrayList<>();
        Optional<OrdenTrabajo> siguiente = cola.extraer();
        while (siguiente.isPresent()) {
            ids.add(siguiente.orElseThrow().id());
            siguiente = cola.extraer();
        }
        return ids;
    }

    /**
     * Crea una orden de trabajo de prueba con los datos mínimos.
     *
     * @param id        identificador de la orden
     * @param prioridad prioridad asignada
     * @return la orden construida
     */
    private static OrdenTrabajo orden(int id, Prioridad prioridad) {
        return new OrdenTrabajo(id, 0, "PRENSA-01", prioridad, "orden de prueba", 0, 4);
    }
}
