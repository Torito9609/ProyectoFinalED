package edu.unal.planta.modelo;

import java.util.Objects;
import java.util.Optional;

/**
 * Técnico de mantenimiento: puede estar libre o atendiendo una orden de trabajo.
 *
 * <p>Cada técnico tiene una especialidad ({@link TipoFallo}). Los técnicos libres esperan en una
 * cola FIFO por especialidad, así que el que lleva más tiempo esperando es el próximo en recibir
 * trabajo. Al terminar una orden, el técnico vuelve <strong>al final</strong> de la cola de su
 * especialidad, con lo que el trabajo se reparte de forma pareja.</p>
 *
 * <p>Si atiende un fallo de la otra especialidad, tarda
 * {@link ConfiguracionPlanta#PENALIDAD_ESPECIALIDAD} veces más.</p>
 *
 * <p>Hay menos técnicos que máquinas a propósito, para que en la demostración se vean filas de
 * órdenes esperando.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Tecnico {

    private final String nombre;
    private final TipoFallo especialidad;
    private OrdenTrabajo ordenActual;
    private int ticksRestantes;

    /**
     * Crea un técnico libre.
     *
     * @param nombre       nombre del técnico
     * @param especialidad tipo de fallo que sabe reparar
     * @throws NullPointerException si el nombre o la especialidad son nulos
     */
    public Tecnico(String nombre, TipoFallo especialidad) {
        this.nombre = Objects.requireNonNull(nombre, "El técnico debe tener nombre");
        this.especialidad = Objects.requireNonNull(especialidad, "El técnico debe tener especialidad");
        this.ordenActual = null;
        this.ticksRestantes = 0;
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
     * Devuelve la especialidad del técnico.
     *
     * @return el tipo de fallo que sabe reparar
     */
    public TipoFallo especialidad() {
        return especialidad;
    }

    /**
     * Indica si el técnico es de la especialidad que pide la orden.
     *
     * @param orden orden a revisar
     * @return {@code true} si la especialidad coincide con el tipo de fallo de la orden
     */
    public boolean esDeLaEspecialidad(OrdenTrabajo orden) {
        return orden.tipoFallo() == especialidad;
    }

    /**
     * Indica si el técnico está disponible para recibir una orden.
     *
     * @return {@code true} si no tiene orden asignada
     */
    public boolean estaLibre() {
        return ordenActual == null;
    }

    /**
     * Devuelve la orden que está atendiendo, si tiene alguna.
     *
     * @return la orden en curso, o un {@link Optional} vacío si está libre
     */
    public Optional<OrdenTrabajo> ordenActual() {
        return Optional.ofNullable(ordenActual);
    }

    /**
     * Devuelve cuántos ticks le faltan para terminar.
     *
     * @return ticks restantes, o cero si está libre
     */
    public int ticksRestantes() {
        return ticksRestantes;
    }

    /**
     * Asigna una orden al técnico. La duración del trabajo la trae la orden; si el técnico no es de
     * la especialidad del fallo, se multiplica por {@link ConfiguracionPlanta#PENALIDAD_ESPECIALIDAD}.
     *
     * @param orden orden a atender
     * @throws IllegalStateException si el técnico ya estaba ocupado
     * @throws NullPointerException  si la orden es nula
     */
    public void asignar(OrdenTrabajo orden) {
        Objects.requireNonNull(orden, "La orden no puede ser nula");
        if (!estaLibre()) {
            throw new IllegalStateException(
                    "El técnico " + nombre + " ya está atendiendo la orden " + ordenActual.id());
        }
        this.ordenActual = orden;
        int penalidad = esDeLaEspecialidad(orden) ? 1 : ConfiguracionPlanta.PENALIDAD_ESPECIALIDAD;
        this.ticksRestantes = orden.duracionEstimadaTicks() * penalidad;
    }

    /**
     * Descuenta un tick al trabajo en curso. No hace nada si el técnico está libre.
     */
    public void avanzar() {
        if (!estaLibre() && ticksRestantes > 0) {
            ticksRestantes--;
        }
    }

    /**
     * Indica si el trabajo en curso ya se completó.
     *
     * @return {@code true} si tiene orden asignada y no le quedan ticks
     */
    public boolean trabajoTerminado() {
        return !estaLibre() && ticksRestantes == 0;
    }

    /**
     * Deja libre al técnico y devuelve la orden que acabó de atender.
     *
     * @return la orden terminada
     * @throws IllegalStateException si el técnico no tenía ninguna orden
     */
    public OrdenTrabajo liberar() {
        if (estaLibre()) {
            throw new IllegalStateException("El técnico " + nombre + " no tiene ninguna orden");
        }
        OrdenTrabajo terminada = ordenActual;
        ordenActual = null;
        ticksRestantes = 0;
        return terminada;
    }

    /**
     * Devuelve al técnico a su estado inicial, para la acción "Reiniciar" de la simulación.
     */
    public void reiniciar() {
        ordenActual = null;
        ticksRestantes = 0;
    }

    /**
     * Devuelve el nombre y en qué está, para la bitácora y la depuración.
     *
     * @return texto con la forma {@code "Ana (libre)"} o {@code "Ana → OT-3, 4 ticks"}
     */
    @Override
    public String toString() {
        if (estaLibre()) {
            return nombre + " (libre)";
        }
        return nombre + " → OT-" + ordenActual.id() + ", " + ticksRestantes + " ticks";
    }
}
