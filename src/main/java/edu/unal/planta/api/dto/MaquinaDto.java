package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.Maquina;
import java.util.List;

/**
 * Estado de una máquina del inventario, con sus sensores y su cola circular de lecturas.
 *
 * @param indice                  posición en el inventario ({@code ArrayList}); es el índice que
 *                                usan las acciones del operador
 * @param etiqueta                etiqueta de la máquina, por ejemplo {@code PRENSA-01}
 * @param tipo                    código del tipo en el catálogo, por ejemplo {@code PRENSA}; es
 *                                el valor que se usa para crear máquinas y para elegir su modelo
 * @param tipoDescripcion         nombre del tipo para mostrar, por ejemplo {@code Prensa hidráulica}
 * @param estado                  nombre del estado, por ejemplo {@code OPERANDO}
 * @param carga                   carga en porcentaje (0–100)
 * @param ticksDesdeMantenimiento ticks desde el último preventivo
 * @param intervaloPreventivo     cada cuántos ticks le toca preventivo
 * @param tieneAnomalia           si tiene un fallo físico en curso
 * @param alarmaActiva            si ya generó un evento que no se ha resuelto
 * @param puntajeFallos           puntaje de fallos acumulado desde el último overhaul; es la clave
 *                                del ranking
 * @param numeroOverhauls         cuántos overhauls ha recibido
 * @param recomendacion           {@code NINGUNA}, {@code OVERHAUL} o {@code REEMPLAZO}
 * @param overhaulPendiente       si tiene una solicitud de overhaul esperando en el montículo; con
 *                                ella no se puede aprobar otra
 * @param sensores                definición de sus sensores
 * @param lecturas                su cola circular de lecturas
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record MaquinaDto(
        int indice,
        String etiqueta,
        String tipo,
        String tipoDescripcion,
        String estado,
        int carga,
        int ticksDesdeMantenimiento,
        int intervaloPreventivo,
        boolean tieneAnomalia,
        boolean alarmaActiva,
        int puntajeFallos,
        int numeroOverhauls,
        String recomendacion,
        boolean overhaulPendiente,
        List<SensorDto> sensores,
        ColaCircularDto lecturas) {

    /**
     * Copia una máquina del modelo.
     *
     * @param indice  posición de la máquina en el inventario
     * @param maquina máquina del modelo
     * @return la copia
     */
    public static MaquinaDto desde(int indice, Maquina maquina) {
        return new MaquinaDto(indice, maquina.etiqueta(), maquina.tipo().name(),
                maquina.tipo().descripcion(), maquina.estado().name(),
                maquina.carga(), maquina.ticksDesdeMantenimiento(), maquina.intervaloPreventivo(),
                maquina.tieneAnomalia(), maquina.alarmaActiva(),
                maquina.puntajeFallos(), maquina.numeroOverhauls(), maquina.recomendacion().name(),
                maquina.overhaulPendiente(),
                maquina.sensores().stream().map(SensorDto::desde).toList(),
                ColaCircularDto.desde(maquina.lecturas()));
    }
}
