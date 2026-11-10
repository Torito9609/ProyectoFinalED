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
 * <p>Un overhaul dura {@link ConfiguracionPlanta#DURACION_OVERHAUL} ticks. Todas las operaciones
 * cuestan O(1).</p>
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
     * Asigna una solicitud al técnico.
     *
     * @param solicitud solicitud de overhaul que va a atender
     * @throws IllegalStateException si el técnico ya estaba ocupado
     * @throws NullPointerException  si la solicitud es nula
     */
    public void asignar(SolicitudOverhaul solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud no puede ser nula");
        if (!estaLibre()) {
            throw new IllegalStateException("El técnico " + nombre + " ya está en un overhaul");
        }
        solicitudActual = solicitud;
        ticksRestantes = ConfiguracionPlanta.DURACION_OVERHAUL;
    }

    /**
     * Descuenta un tick al overhaul en curso. No hace nada si el técnico está libre.
     */
    public void avanzar() {
        if (!estaLibre() && ticksRestantes > 0) {
            ticksRestantes--;
        }
    }

    /**
     * Indica si el overhaul en curso ya terminó.
     *
     * @return {@code true} si tiene solicitud y no le quedan ticks
     */
    public boolean trabajoTerminado() {
        return !estaLibre() && ticksRestantes == 0;
    }

    /**
     * Deja libre al técnico y devuelve la solicitud que acabó de atender.
     *
     * @return la solicitud terminada
     * @throws IllegalStateException si el técnico no tenía ninguna
     */
    public SolicitudOverhaul liberar() {
        if (estaLibre()) {
            throw new IllegalStateException("El técnico " + nombre + " no tiene ningún overhaul");
        }
        SolicitudOverhaul terminada = solicitudActual;
        reiniciar();
        return terminada;
    }

    /**
     * Deja al técnico libre, para una partida nueva.
     */
    public void reiniciar() {
        solicitudActual = null;
        ticksRestantes = 0;
    }
}
