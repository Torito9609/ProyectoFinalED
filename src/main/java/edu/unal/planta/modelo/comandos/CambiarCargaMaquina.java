package edu.unal.planta.modelo.comandos;

import edu.unal.planta.modelo.Maquina;
import java.util.Objects;

/**
 * Cambia la carga de trabajo de una máquina.
 *
 * <p>La carga influye directamente en la probabilidad de fallo, así que subirla es la forma que
 * tiene el operador de provocar más averías durante la demostración.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class CambiarCargaMaquina implements ComandoOperador {

    private final Maquina maquina;
    private final int cargaAnterior;
    private final int cargaNueva;

    /**
     * Prepara el cambio de carga.
     *
     * @param maquina    máquina afectada
     * @param cargaNueva porcentaje nuevo, entre 0 y 100
     * @throws IllegalArgumentException si el porcentaje está fuera de rango
     * @throws NullPointerException     si la máquina es nula
     */
    public CambiarCargaMaquina(Maquina maquina, int cargaNueva) {
        this.maquina = Objects.requireNonNull(maquina, "La máquina no puede ser nula");
        if (cargaNueva < 0 || cargaNueva > 100) {
            throw new IllegalArgumentException("La carga debe estar entre 0 y 100: " + cargaNueva);
        }
        this.cargaAnterior = maquina.carga();
        this.cargaNueva = cargaNueva;
    }

    @Override
    public void ejecutar() {
        maquina.cambiarCarga(cargaNueva);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Solo se puede deshacer si la carga sigue siendo la que dejó este comando, es decir si
     * nadie la cambió después.</p>
     */
    @Override
    public boolean sePuedeDeshacer() {
        return maquina.carga() == cargaNueva;
    }

    @Override
    public void deshacer() {
        if (!sePuedeDeshacer()) {
            throw new IllegalStateException("La carga de " + maquina.etiqueta()
                    + " ya cambió; no se puede deshacer");
        }
        maquina.cambiarCarga(cargaAnterior);
    }

    @Override
    public String descripcion() {
        return "Carga " + maquina.etiqueta() + ": " + cargaAnterior + "% → " + cargaNueva + "%";
    }
}
