package edu.unal.planta.controlador;

import edu.unal.planta.modelo.ConfiguracionPlanta;
import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Magnitud;
import edu.unal.planta.modelo.Maquina;
import edu.unal.planta.modelo.Planta;
import edu.unal.planta.modelo.comandos.CambiarCargaMaquina;
import edu.unal.planta.modelo.comandos.PararOArrancarMaquina;
import edu.unal.planta.vista.AccionesOperador;
import java.util.Objects;
import javax.swing.Timer;

/**
 * Traduce lo que pide el operador en llamadas al modelo, y es dueño del reloj de la simulación.
 *
 * <p>Implementa {@link AccionesOperador}, que es una interfaz declarada en el paquete de la vista.
 * Gracias a eso <strong>esta clase no importa nada de Swing salvo el temporizador</strong>, y la
 * vista no importa nada de este paquete: los dos lados dependen del contrato, no uno del otro.</p>
 *
 * <h2>Por qué {@code javax.swing.Timer}</h2>
 * <p>Este temporizador dispara sus eventos en el hilo de despacho de Swing, el mismo donde se
 * dibuja la interfaz. Como la simulación y el repintado ocurren en el mismo hilo, nunca se dibuja
 * una planta a medio actualizar y no hay condiciones de carrera que manejar. Con {@code Thread} o
 * {@code java.util.Timer} habría que sincronizar el modelo, y eso en un proyecto de un solo hilo
 * sería complejidad sin ninguna ganancia.</p>
 *
 * <p>El controlador no toca la vista: después de cada cambio es la planta la que avisa a sus
 * observadores, y la ventana es uno de ellos.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class ControladorPlanta implements AccionesOperador {

    private final Planta planta;
    private final Timer reloj;

    /**
     * Crea el controlador de una planta, con el reloj detenido.
     *
     * @param planta planta que va a dirigir
     * @throws NullPointerException si la planta es nula
     */
    public ControladorPlanta(Planta planta) {
        this.planta = Objects.requireNonNull(planta, "La planta no puede ser nula");
        this.reloj = new Timer(ConfiguracionPlanta.MILIS_POR_TICK, evento -> planta.tick());
        this.reloj.setInitialDelay(0);
    }

    @Override
    public void alternarEjecucion() {
        if (reloj.isRunning()) {
            reloj.stop();
        } else {
            reloj.start();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Si el reloj estaba corriendo se detiene primero: dar un paso tiene sentido cuando se
     * quiere observar con calma qué cambió, y eso no funciona si la simulación sigue avanzando.</p>
     */
    @Override
    public void paso() {
        reloj.stop();
        planta.tick();
    }

    @Override
    public void reiniciar() {
        reloj.stop();
        planta.reiniciar();
    }

    /**
     * {@inheritDoc}
     *
     * <p>El comando se construye <strong>justo antes</strong> de ejecutarse y no se guarda nunca,
     * porque al crearse toma nota del estado actual de la máquina. Un comando guardado recordaría
     * un estado viejo y al deshacerlo restauraría algo equivocado.</p>
     *
     * <p>Si la máquina no está operando ni parada a mano, la acción no aplica y se ignora. La vista
     * además deshabilita el botón en ese caso, así que esto es solo una red de seguridad.</p>
     */
    @Override
    public void pararOArrancar(int indiceMaquina) {
        Maquina maquina = planta.maquina(indiceMaquina);
        boolean aplicable = maquina.estado() == EstadoMaquina.OPERANDO
                || maquina.estado() == EstadoMaquina.PARADA_MANUAL;
        if (aplicable) {
            planta.ejecutar(new PararOArrancarMaquina(maquina));
        }
    }

    @Override
    public void cambiarCarga(int indiceMaquina, int carga) {
        Maquina maquina = planta.maquina(indiceMaquina);
        if (maquina.carga() != carga) {
            planta.ejecutar(new CambiarCargaMaquina(maquina, carga));
        }
    }

    @Override
    public void inyectarFallo(int indiceMaquina, int indiceSensor, Magnitud magnitud) {
        planta.inyectarFallo(indiceMaquina, indiceSensor, magnitud);
    }

    @Override
    public void deshacer() {
        planta.deshacer();
    }

    @Override
    public boolean estaCorriendo() {
        return reloj.isRunning();
    }

    /**
     * Detiene el reloj, sin tocar el estado de la planta.
     *
     * <p>Lo usan las pruebas para no dejar un temporizador vivo.</p>
     */
    public void detener() {
        reloj.stop();
    }
}
