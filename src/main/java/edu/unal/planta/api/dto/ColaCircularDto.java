package edu.unal.planta.api.dto;

import edu.unal.planta.estructuras.ColaCircularSoloLectura;
import edu.unal.planta.modelo.Lectura;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Foto de la cola circular de lecturas de una máquina, tal como está en memoria.
 *
 * <p>Las celdas van en <strong>orden físico</strong> (índice 0 a capacidad − 1), no en orden
 * lógico, y una celda vacía viaja como {@code null}. Así el frontend puede dibujar el arreglo fijo
 * con los punteros {@code frente} y {@code final}, y se ve al final dar la vuelta.</p>
 *
 * @param capacidad      número fijo de celdas
 * @param tamano         cuántas celdas tienen una lectura
 * @param indiceFrente   celda de la lectura más antigua
 * @param indiceFinal    celda donde se escribirá la próxima lectura
 * @param sobrescrituras cuántas lecturas viejas se han sobrescrito
 * @param celdas         una entrada por celda, en orden físico; {@code null} si la celda está vacía
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ColaCircularDto(
        int capacidad,
        int tamano,
        int indiceFrente,
        int indiceFinal,
        long sobrescrituras,
        List<LecturaDto> celdas) {

    /**
     * Copia la cola circular celda por celda.
     *
     * <p>No se usa {@code List.copyOf} porque rechaza los {@code null}, y aquí una celda vacía es
     * información: por eso se copia a una lista nueva y se envuelve como no modificable.</p>
     *
     * <p>Complejidad: O(capacidad).</p>
     *
     * @param cola cola circular del modelo, en modo de solo lectura
     * @return la copia
     */
    public static ColaCircularDto desde(ColaCircularSoloLectura<Lectura> cola) {
        List<LecturaDto> celdas = new ArrayList<>(cola.capacidad());
        for (int i = 0; i < cola.capacidad(); i++) {
            celdas.add(cola.contenidoCelda(i).map(LecturaDto::desde).orElse(null));
        }
        return new ColaCircularDto(cola.capacidad(), cola.tamano(), cola.indiceFrente(),
                cola.indiceFinal(), cola.sobrescrituras(), Collections.unmodifiableList(celdas));
    }
}
