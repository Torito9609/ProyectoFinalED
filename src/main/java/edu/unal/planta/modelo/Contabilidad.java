package edu.unal.planta.modelo;

/**
 * La economía de una compañía: su caja, lo que ha ganado y gastado, y cuánto produjeron sus
 * máquinas.
 *
 * <p>El dinero se lleva en números enteros ({@code long}), así las cuentas son exactas y la misma
 * semilla da siempre el mismo resultado, sin errores de redondeo.</p>
 *
 * <p>Si la caja baja de 0, la compañía queda en quiebra: la {@link Planta} deja de operar. Todas
 * las operaciones cuestan O(1).</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class Contabilidad {

    private final long presupuestoInicial;
    private long caja;
    private long ingresos;
    private long costos;
    private long costoDelTick;
    private long ticksProduciendo;
    private long ticksPosibles;

    /**
     * Crea la contabilidad con la caja en el presupuesto inicial.
     *
     * @param presupuestoInicial dinero con que arranca la partida; no puede ser negativo
     * @throws IllegalArgumentException si el presupuesto es negativo
     */
    public Contabilidad(long presupuestoInicial) {
        if (presupuestoInicial < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto inicial no puede ser negativo: " + presupuestoInicial);
        }
        this.presupuestoInicial = presupuestoInicial;
        reiniciar();
    }

    /**
     * Empieza la cuenta de un tick nuevo: el costo del tick vuelve a 0.
     *
     * <p>Lo llama la planta <strong>al registrar el tick en el historial</strong>, no al empezar a
     * contabilizar: así el costo de un tick incluye también lo que se cobra fuera del paso 7b, como
     * el overhaul que despacha la organización o un reemplazo hecho a mano entre dos ticks.</p>
     */
    public void iniciarTick() {
        costoDelTick = 0;
    }

    /**
     * Suma dinero a la caja.
     *
     * @param monto dinero que entra; no puede ser negativo
     * @throws IllegalArgumentException si el monto es negativo
     */
    public void ingresar(long monto) {
        exigirNoNegativo(monto);
        caja += monto;
        ingresos += monto;
    }

    /**
     * Saca dinero de la caja. Puede dejarla bajo 0: eso es la quiebra.
     *
     * @param monto dinero que sale; no puede ser negativo
     * @throws IllegalArgumentException si el monto es negativo
     */
    public void cobrar(long monto) {
        exigirNoNegativo(monto);
        caja -= monto;
        costos += monto;
        costoDelTick += monto;
    }

    /**
     * Anota si una máquina produjo en este tick, para calcular la disponibilidad.
     *
     * @param produjo {@code true} si la máquina estaba produciendo
     */
    public void registrarMaquina(boolean produjo) {
        ticksPosibles++;
        if (produjo) {
            ticksProduciendo++;
        }
    }

    /**
     * Indica si la compañía está en quiebra.
     *
     * @return {@code true} si la caja es negativa
     */
    public boolean enQuiebra() {
        return caja < 0;
    }

    /**
     * Devuelve el dinero disponible.
     *
     * @return la caja; negativa si la compañía está en quiebra
     */
    public long caja() {
        return caja;
    }

    /**
     * Devuelve todo lo ingresado en la partida.
     *
     * @return ingresos acumulados
     */
    public long ingresos() {
        return ingresos;
    }

    /**
     * Devuelve todo lo gastado en la partida.
     *
     * @return costos acumulados
     */
    public long costos() {
        return costos;
    }

    /**
     * Devuelve lo gastado desde el último registro en el historial. Es lo que el historial guarda
     * por tick.
     *
     * @return costo del tick
     */
    public long costoDelTick() {
        return costoDelTick;
    }

    /**
     * Devuelve la fracción del tiempo en que las máquinas produjeron.
     *
     * @return disponibilidad entre 0 y 1; 1 si todavía no hay ticks contados
     */
    public double disponibilidad() {
        return ticksPosibles == 0 ? 1.0 : (double) ticksProduciendo / ticksPosibles;
    }

    /**
     * Devuelve la caja al presupuesto inicial y borra los acumulados, para una partida nueva.
     */
    public void reiniciar() {
        caja = presupuestoInicial;
        ingresos = 0;
        costos = 0;
        costoDelTick = 0;
        ticksProduciendo = 0;
        ticksPosibles = 0;
    }

    private static void exigirNoNegativo(long monto) {
        if (monto < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo: " + monto);
        }
    }
}
