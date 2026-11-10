package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Planta;
import edu.unal.planta.modelo.Tecnico;
import java.util.ArrayList;
import java.util.List;

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
 * @param maquinas       el inventario ({@code ArrayList}), en orden de índice
 * @param eventos        la cola FIFO de eventos, del primero al último en salir
 * @param tecnicos       todos los técnicos, libres u ocupados
 * @param tecnicosLibres nombres de la cola FIFO de técnicos libres, del primero al último en salir
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
        List<MaquinaDto> maquinas,
        List<EventoFalloDto> eventos,
        List<TecnicoDto> tecnicos,
        List<String> tecnicosLibres,
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
                List.copyOf(maquinas),
                planta.eventosPendientes().stream().map(EventoFalloDto::desde).toList(),
                planta.tecnicos().stream().map(TecnicoDto::desde).toList(),
                planta.tecnicosLibres().stream().map(Tecnico::nombre).toList(),
                ColaPrioridadDto.desde(planta.ordenes()),
                List.copyOf(planta.accionesDeshacibles()),
                List.copyOf(planta.bitacora()));
    }
}
