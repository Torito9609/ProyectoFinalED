package edu.unal.planta.servicio;

import edu.unal.planta.modelo.Planta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Único dueño del modelo y del reloj de la simulación.
 *
 * <h2>Por qué existe</h2>
 * <p>En la Entrega 1 todo ocurría en un solo hilo, el de eventos de Swing, y no había concurrencia.
 * Con Spring Boot ya no es así: Tomcat atiende cada petición HTTP en un hilo distinto y el reloj
 * corre en otro. Si dos hilos tocaran la {@link Planta} a la vez, una petición podría ver un tick a
 * medias, o dos acciones podrían mezclarse.</p>
 *
 * <p>La solución es la regla 4 de la sección 4 de {@code CLAUDE.md}: esta clase es la
 * <strong>única</strong> que guarda la planta, nunca la entrega hacia afuera y
 * <strong>todos sus métodos públicos son {@code synchronized}</strong>, incluido el del reloj. Así,
 * en cada momento solo un hilo trabaja con el modelo. Es la misma garantía que daba el hilo de
 * eventos de Swing, pero dicha explícitamente.</p>
 *
 * <h2>El reloj</h2>
 * <p>No se crean hilos a mano: Spring llama a {@link #alDispararReloj()} cada cierto número de
 * milisegundos, configurado en {@code application.properties} ({@code planta.intervalo-ms}). El
 * reloj dispara siempre; esta clase decide si avanza la simulación según si está en ejecución o en
 * pausa.</p>
 *
 * <p>Todas las operaciones cuestan lo que cuesta la operación del modelo que llaman: O(1) las de
 * control, y lo que cueste un {@link Planta#tick()} las que avanzan la simulación.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@Service
public class ServicioSimulacion {

    /** La planta simulada. Solo esta clase la conoce. */
    private final Planta planta;

    /** {@code true} si el reloj debe avanzar la simulación; {@code false} si está en pausa. */
    private boolean enEjecucion;

    /**
     * Crea el servicio con una planta nueva, en pausa.
     *
     * <p>Spring llama a este constructor y le pasa la semilla de {@code application.properties}.
     * Las pruebas lo llaman directamente, sin Spring.</p>
     *
     * @param semilla semilla del generador aleatorio; la misma semilla repite la simulación
     */
    public ServicioSimulacion(@Value("${planta.semilla}") long semilla) {
        this.planta = new Planta(semilla);
        this.enEjecucion = false;
    }

    /**
     * Pone la simulación en ejecución: desde el siguiente disparo, el reloj avanza un tick cada vez.
     */
    public synchronized void iniciar() {
        enEjecucion = true;
    }

    /**
     * Pone la simulación en pausa. El estado de la planta no cambia.
     */
    public synchronized void pausar() {
        enEjecucion = false;
    }

    /**
     * Avanza exactamente un tick.
     *
     * <p>Primero pausa, como en la Entrega 1: dar un paso sirve para observar con calma qué cambió,
     * y eso no funciona si el reloj sigue avanzando.</p>
     */
    public synchronized void paso() {
        enEjecucion = false;
        planta.tick();
    }

    /**
     * Pausa y devuelve la planta a su estado inicial, con la misma semilla.
     */
    public synchronized void reiniciar() {
        enEjecucion = false;
        planta.reiniciar();
    }

    /**
     * Lo llama el reloj de Spring a intervalo fijo. Avanza un tick solo si la simulación está en
     * ejecución.
     *
     * <p>Es {@code synchronized} como los demás: un tick del reloj nunca se mezcla con una
     * petición.</p>
     */
    @Scheduled(fixedRateString = "${planta.intervalo-ms}")
    public synchronized void alDispararReloj() {
        if (enEjecucion) {
            planta.tick();
        }
    }

    /**
     * Indica si la simulación está en ejecución.
     *
     * @return {@code true} si el reloj avanza la simulación; {@code false} si está en pausa
     */
    public synchronized boolean enEjecucion() {
        return enEjecucion;
    }

    /**
     * Devuelve el tick actual de la simulación.
     *
     * @return número de ticks ejecutados desde el inicio o el último reinicio
     */
    public synchronized int tickActual() {
        return planta.tickActual();
    }

    /**
     * Devuelve la semilla con que corre la simulación.
     *
     * @return semilla del generador aleatorio
     */
    public synchronized long semilla() {
        return planta.semilla();
    }
}
