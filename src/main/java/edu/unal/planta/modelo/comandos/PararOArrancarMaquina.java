package edu.unal.planta.modelo.comandos;

import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Maquina;
import java.util.Objects;

/**
 * Para o arranca una máquina a mano: alterna entre OPERANDO y PARADA_MANUAL.
 *
 * <p>Guarda el estado que tenía la máquina antes y el que le dejó, que es lo que necesita para
 * revertirse y para saber si todavía puede hacerlo.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PararOArrancarMaquina implements ComandoOperador {

    private final Maquina maquina;
    private final EstadoMaquina estadoAnterior;
    private final EstadoMaquina estadoNuevo;

    /**
     * Prepara la acción sobre una máquina.
     *
     * @param maquina máquina a parar o arrancar
     * @throws IllegalStateException si la máquina no está operando ni parada a mano, porque
     *                              entonces la está atendiendo la simulación
     * @throws NullPointerException  si la máquina es nula
     */
    public PararOArrancarMaquina(Maquina maquina) {
        this.maquina = Objects.requireNonNull(maquina, "La máquina no puede ser nula");
        this.estadoAnterior = maquina.estado();
        this.estadoNuevo = switch (estadoAnterior) {
            case OPERANDO -> EstadoMaquina.PARADA_MANUAL;
            case PARADA_MANUAL -> EstadoMaquina.OPERANDO;
            default -> throw new IllegalStateException("No se puede parar ni arrancar "
                    + maquina.etiqueta() + " porque está " + estadoAnterior.descripcion());
        };
    }

    @Override
    public void ejecutar() {
        maquina.cambiarEstado(estadoNuevo);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Solo se puede deshacer si la máquina sigue en el estado que este comando le dejó. Si una
     * alarma crítica la detuvo, o un técnico la está atendiendo, revertir produciría un estado
     * inconsistente.</p>
     */
    @Override
    public boolean sePuedeDeshacer() {
        return maquina.estado() == estadoNuevo;
    }

    @Override
    public void deshacer() {
        if (!sePuedeDeshacer()) {
            throw new IllegalStateException("La simulación ya cambió el estado de "
                    + maquina.etiqueta() + "; no se puede deshacer");
        }
        maquina.cambiarEstado(estadoAnterior);
    }

    @Override
    public String descripcion() {
        String accion = estadoNuevo == EstadoMaquina.PARADA_MANUAL ? "Parar" : "Arrancar";
        return accion + " " + maquina.etiqueta();
    }
}
