package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Planta;
import edu.unal.planta.modelo.Tecnico;
import edu.unal.planta.modelo.TipoFallo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado completo de la planta de una compañía: lo que el frontend necesita para dibujar todos los
 * paneles.
 *
 * <p>Es una foto: se arma copiando el modelo dentro de un método {@code synchronized} del
 * servicio, así que es consistente (nunca muestra un tick a medias) y no cambia después.</p>
 *
 * @param idCompania     id de la compañía: su índice en la organización
 * @param nombre         nombre de la compañía
 * @param tick           tick actual
 * @param semilla        semilla de esta compañía, derivada de la maestra
 * @param enEjecucion    si el reloj está avanzando la simulación
 * @param modo           {@code AUTOMATICO} o {@code MANUAL}; los botones de clasificar y asignar
 *                       solo funcionan en manual
 * @param caja           dinero disponible; negativo si está en quiebra
 * @param ingresos       todo lo ingresado en la partida
 * @param costos         todo lo gastado en la partida
 * @param enQuiebra      si la compañía está en quiebra: no opera ni acepta acciones
 * @param maquinas       el inventario ({@code ArrayList}), en orden de índice
 * @param eventos        la cola FIFO de eventos, del primero al último en salir
 * @param tecnicos       todos los técnicos, libres u ocupados
 * @param tecnicosLibres las dos colas FIFO de técnicos libres, por especialidad ({@code MECANICO} y
 *                       {@code ELECTRICO}); en cada una, los nombres del primero al último en salir
 * @param colaPrioridad  la cola con prioridad de órdenes
 * @param pilaDeshacer   descripciones de la pila de deshacer, de la cima hacia abajo
 * @param bitacora       líneas de la bitácora, de la más antigua a la más reciente
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EstadoPlantaDto(
        int idCompania,
        String nombre,
        int tick,
        long semilla,
        boolean enEjecucion,
        String modo,
        long caja,
        long ingresos,
        long costos,
        boolean enQuiebra,
        List<MaquinaDto> maquinas,
        List<EventoFalloDto> eventos,
        List<TecnicoDto> tecnicos,
        Map<String, List<String>> tecnicosLibres,
        ColaPrioridadDto colaPrioridad,
        List<String> pilaDeshacer,
        List<String> bitacora) {

    /**
     * Copia el estado completo de una planta.
     *
     * <p>Quien la llame debe tener el acceso exclusivo a la planta mientras copia; en la aplicación
     * es {@code ServicioSimulacion}, desde un método {@code synchronized}.</p>
     *
     * <p>Complejidad: O(n) en el tamaño total del estado.</p>
     *
     * @param planta      planta del modelo
     * @param enEjecucion si la simulación está en ejecución
     * @return la foto del estado
     */
    public static EstadoPlantaDto desde(Planta planta, boolean enEjecucion) {
        List<MaquinaDto> maquinas = new ArrayList<>(planta.inventario().size());
        for (int i = 0; i < planta.inventario().size(); i++) {
            maquinas.add(MaquinaDto.desde(i, planta.maquina(i)));
        }
        return new EstadoPlantaDto(
                planta.idCompania(),
                planta.nombre(),
                planta.tickActual(),
                planta.semilla(),
                enEjecucion,
                planta.modo().name(),
                planta.caja(),
                planta.ingresos(),
                planta.costos(),
                planta.enQuiebra(),
                List.copyOf(maquinas),
                planta.eventosPendientes().stream().map(EventoFalloDto::desde).toList(),
                planta.tecnicos().stream().map(TecnicoDto::desde).toList(),
                colasDeTecnicos(planta),
                ColaPrioridadDto.desde(planta.ordenes()),
                List.copyOf(planta.accionesDeshacibles()),
                List.copyOf(planta.bitacora()));
    }

    /** Copia las dos colas de técnicos libres, en el orden del enumerado. */
    private static Map<String, List<String>> colasDeTecnicos(Planta planta) {
        Map<String, List<String>> colas = new LinkedHashMap<>();
        for (TipoFallo especialidad : TipoFallo.values()) {
            colas.put(especialidad.name(),
                    planta.tecnicosLibres(especialidad).stream().map(Tecnico::nombre).toList());
        }
        return Collections.unmodifiableMap(colas);
    }
}
