package edu.unal.planta.modelo;

import java.util.Objects;
import java.util.Optional;

/**
 * Técnico del fabricante: hace los overhauls de todas las compañías.
 *
 * <p>Son pocos y compartidos. Los libres esperan en una cola FIFO de la {@link Organizacion}, y al
 * terminar un overhaul vuelven al final de esa cola, igual que los técnicos internos. Se separa de
 * {@link Tecnico} porque atiende solicitudes de overhaul, no órdenes de trabajo, y no tiene
 * especialidad.</p>
 *
 * <p>Asignar, avanzar y terminar un overhaul llegan con la ejecución de los overhauls en el tick.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class TecnicoExterno {

    private final String nombre;
    private SolicitudOverhaul solicitudActual;
    private int ticksRestantes;

    /**
     * Crea un técnico del fabricante libre.
     *
     * @param nombre nombre del técnico
     * @throws NullPointerException si el nombre es nulo
     */
    public TecnicoExterno(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "El técnico debe tener nombre");
        reiniciar();
    }

    /**
     * Devuelve el nombre del técnico.
     *
     * @return nombre
     */
    public String nombre() {
        return nombre;
    }

    /**
     * Indica si el técnico está disponible para un overhaul.
     *
     * @return {@code true} si no está atendiendo ninguna solicitud
     */
    public boolean estaLibre() {
        return solicitudActual == null;
    }

    /**
     * Devuelve la solicitud que está atendiendo, si tiene alguna.
     *
     * @return la solicitud en curso, o un {@link Optional} vacío si está libre
     */
    public Optional<SolicitudOverhaul> solicitudActual() {
        return Optional.ofNullable(solicitudActual);
    }

    /**
     * Devuelve cuántos ticks le faltan para terminar el overhaul.
     *
     * @return ticks restantes, o cero si está libre
     */
    public int ticksRestantes() {
        return ticksRestantes;
    }

    /**
     * Deja al técnico libre, para una partida nueva.
     */
    public void reiniciar() {
        solicitudActual = null;
        ticksRestantes = 0;
    }
}
