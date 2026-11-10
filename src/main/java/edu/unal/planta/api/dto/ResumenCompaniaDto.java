package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Maquina;
import edu.unal.planta.modelo.Planta;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Totales de una compañía, para la lista del dashboard del superusuario.
 *
 * <p>Es una foto, como {@link EstadoPlantaDto}: se arma dentro de un método {@code synchronized} del
 * servicio y no cambia después.</p>
 *
 * @param id                id de la compañía: su índice en la organización
 * @param nombre            nombre de la compañía
 * @param numeroMaquinas    tamaño del inventario
 * @param maquinasPorEstado cuántas máquinas hay en cada estado; trae todos los estados, en el
 *                          orden del enumerado, incluso los que tienen 0
 * @param eventosPendientes tamaño de la cola de eventos por clasificar
 * @param ordenesPendientes tamaño de la cola con prioridad (los cuatro carriles)
 * @param tecnicosLibres    técnicos libres, sumando las dos colas de especialidad
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record ResumenCompaniaDto(
        int id,
        String nombre,
        int numeroMaquinas,
        Map<String, Integer> maquinasPorEstado,
        int eventosPendientes,
        int ordenesPendientes,
        int tecnicosLibres) {

    /**
     * Calcula los totales de una compañía.
     *
     * <p>Complejidad: O(m), con m el número de máquinas: un recorrido del inventario para contar
     * los estados. Los tamaños de las colas son O(1).</p>
     *
     * @param planta planta de la compañía
     * @return el resumen
     */
    public static ResumenCompaniaDto desde(Planta planta) {
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoMaquina estado : EstadoMaquina.values()) {
            porEstado.put(estado.name(), 0);
        }
        for (Maquina maquina : planta.inventario()) {
            porEstado.merge(maquina.estado().name(), 1, Integer::sum);
        }
        return new ResumenCompaniaDto(
                planta.idCompania(),
                planta.nombre(),
                planta.inventario().size(),
                Collections.unmodifiableMap(porEstado),
                planta.eventosPendientes().size(),
                planta.ordenes().tamano(),
                planta.totalTecnicosLibres());
    }
}
