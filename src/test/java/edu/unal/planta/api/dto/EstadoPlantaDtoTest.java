package edu.unal.planta.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unal.planta.estructuras.ColaCircularSoloLectura;
import edu.unal.planta.modelo.Lectura;
import edu.unal.planta.modelo.Planta;
import edu.unal.planta.modelo.Prioridad;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los DTOs: que sean copias del modelo y no vistas de él.
 */
@DisplayName("EstadoPlantaDto")
class EstadoPlantaDtoTest {

    private Planta planta;

    @BeforeEach
    void crearPlanta() {
        planta = new Planta(7L);
        for (int i = 0; i < 5; i++) {
            planta.tick();
        }
    }

    @Test
    @DisplayName("es una copia: si la planta sigue avanzando, la foto no cambia")
    void esUnaCopia() {
        EstadoPlantaDto foto = EstadoPlantaDto.desde(planta, false);
        int tick = foto.tick();
        int lineasBitacora = foto.bitacora().size();
        List<LecturaDto> celdas = new ArrayList<>(foto.maquinas().get(0).lecturas().celdas());

        for (int i = 0; i < 20; i++) {
            planta.tick();
        }

        assertEquals(tick, foto.tick());
        assertEquals(lineasBitacora, foto.bitacora().size());
        assertEquals(celdas, foto.maquinas().get(0).lecturas().celdas());
    }

    @Test
    @DisplayName("sus listas no se pueden modificar")
    void listasNoModificables() {
        EstadoPlantaDto foto = EstadoPlantaDto.desde(planta, false);

        assertThrows(UnsupportedOperationException.class, () -> foto.maquinas().clear());
        assertThrows(UnsupportedOperationException.class, () -> foto.bitacora().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> foto.maquinas().get(0).lecturas().celdas().set(0, null));
        assertThrows(UnsupportedOperationException.class,
                () -> foto.colaPrioridad().carriles().clear());
    }

    @Test
    @DisplayName("la cola circular conserva los índices y las celdas vacías, en orden físico")
    void colaCircularFiel() {
        ColaCircularSoloLectura<Lectura> original = planta.maquina(0).lecturas();
        ColaCircularDto copia = EstadoPlantaDto.desde(planta, false).maquinas().get(0).lecturas();

        assertEquals(original.capacidad(), copia.celdas().size());
        assertEquals(original.indiceFrente(), copia.indiceFrente());
        assertEquals(original.indiceFinal(), copia.indiceFinal());
        assertEquals(original.sobrescrituras(), copia.sobrescrituras());
        assertTrue(copia.tamano() < copia.capacidad(), "tras 5 ticks debe haber celdas vacías");
        for (int i = 0; i < original.capacidad(); i++) {
            int celda = i;
            original.contenidoCelda(i).ifPresentOrElse(
                    lectura -> assertEquals(lectura.tick(), copia.celdas().get(celda).tick()),
                    () -> assertNull(copia.celdas().get(celda)));
        }
    }

    @Test
    @DisplayName("la cola con prioridad viaja como cuatro carriles, de P1 a P4")
    void cuatroCarriles() {
        ColaPrioridadDto cola = EstadoPlantaDto.desde(planta, false).colaPrioridad();

        assertEquals(Prioridad.NIVELES, cola.carriles().size());
        for (int i = 0; i < Prioridad.NIVELES; i++) {
            assertEquals(Prioridad.porNivel(i).name(), cola.carriles().get(i).prioridad());
        }
    }
}
