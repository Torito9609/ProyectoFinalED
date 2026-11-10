package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.EventoFallo;

/**
 * Un evento de la cola FIFO de eventos: una detección cruda, todavía sin prioridad.
 *
 * @param indiceMaquina   posición de la máquina en el inventario
 * @param etiquetaMaquina etiqueta de la máquina
 * @param indiceSensor    posición del sensor en la máquina
 * @param nombreSensor    nombre del sensor que se salió de rango
 * @param valorLeido      valor que disparó la detección
 * @param tick            tick en que se detectó
 * @param tipoFallo       {@code MECANICO} o {@code ELECTRICO}
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record EventoFalloDto(
        int indiceMaquina,
        String etiquetaMaquina,
        int indiceSensor,
        String nombreSensor,
        double valorLeido,
        int tick,
        String tipoFallo) {

    /**
     * Copia un evento del modelo.
     *
     * @param evento evento del modelo
     * @return la copia
     */
    public static EventoFalloDto desde(EventoFallo evento) {
        return new EventoFalloDto(evento.indiceMaquina(), evento.etiquetaMaquina(),
                evento.indiceSensor(), evento.nombreSensor(), evento.valorLeido(), evento.tick(),
                evento.tipoFallo().name());
    }
}
