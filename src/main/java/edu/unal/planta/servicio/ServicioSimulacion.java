package edu.unal.planta.servicio;

import edu.unal.planta.api.dto.EstadoPlantaDto;
import edu.unal.planta.api.dto.EstadoSimulacionDto;
import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Magnitud;
import edu.unal.planta.modelo.Maquina;
import edu.unal.planta.modelo.Planta;
import edu.unal.planta.modelo.comandos.CambiarCargaMaquina;
import edu.unal.planta.modelo.comandos.PararOArrancarMaquina;
import java.util.Locale;
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
     *
     * @return el estado de la simulación después de la acción
     */
    public synchronized EstadoSimulacionDto iniciar() {
        enEjecucion = true;
        return estadoSimulacion();
    }

    /**
     * Pone la simulación en pausa. El estado de la planta no cambia.
     *
     * @return el estado de la simulación después de la acción
     */
    public synchronized EstadoSimulacionDto pausar() {
        enEjecucion = false;
        return estadoSimulacion();
    }

    /**
     * Avanza exactamente un tick.
     *
     * <p>Primero pausa, como en la Entrega 1: dar un paso sirve para observar con calma qué cambió,
     * y eso no funciona si el reloj sigue avanzando.</p>
     *
     * @return el estado de la simulación después de la acción
     */
    public synchronized EstadoSimulacionDto paso() {
        enEjecucion = false;
        planta.tick();
        return estadoSimulacion();
    }

    /**
     * Pausa y devuelve la planta a su estado inicial, con la misma semilla.
     *
     * @return el estado de la simulación después de la acción
     */
    public synchronized EstadoSimulacionDto reiniciar() {
        enEjecucion = false;
        planta.reiniciar();
        return estadoSimulacion();
    }

    // ---------------------------------------------------------------------------------
    // Acciones del operador
    // ---------------------------------------------------------------------------------

    /**
     * Inyecta un fallo en una máquina, para forzar un caso en la demostración.
     *
     * <p>Si la máquina ya tiene una anomalía o una alarma activa, la acción no se permite. En ese
     * caso igual se llama al modelo, para que registre el rechazo en la bitácora como en la
     * Entrega 1, y después se lanza la excepción.</p>
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @param indiceSensor  posición del sensor en la máquina
     * @param magnitud      {@code LEVE}, {@code MODERADA} o {@code SEVERA}
     * @return el estado de la planta después de la acción
     * @throws NoEncontradoException      si la máquina o el sensor no existen
     * @throws IllegalArgumentException   si la magnitud no existe
     * @throws AccionNoPermitidaException si la máquina ya tiene un fallo activo
     */
    public synchronized EstadoPlantaDto inyectarFallo(int indiceMaquina, int indiceSensor,
                                                      String magnitud) {
        Maquina maquina = buscarMaquina(indiceMaquina);
        if (indiceSensor < 0 || indiceSensor >= maquina.sensores().size()) {
            throw new NoEncontradoException("La máquina " + maquina.etiqueta()
                    + " no tiene un sensor con índice " + indiceSensor);
        }
        Magnitud valor = convertirMagnitud(magnitud);
        boolean rechazada = maquina.tieneAnomalia() || maquina.alarmaActiva();
        planta.inyectarFallo(indiceMaquina, indiceSensor, valor);
        if (rechazada) {
            throw new AccionNoPermitidaException("No se puede inyectar un fallo en "
                    + maquina.etiqueta() + ": ya tiene un fallo activo");
        }
        return estadoPlanta();
    }

    /**
     * Para una máquina que está operando, o arranca una que está parada a mano. Se apila para
     * poder deshacerla.
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @return el estado de la planta después de la acción
     * @throws NoEncontradoException      si la máquina no existe
     * @throws AccionNoPermitidaException si la máquina está en otro estado, porque entonces la
     *                                    está atendiendo la simulación
     */
    public synchronized EstadoPlantaDto pararOArrancar(int indiceMaquina) {
        Maquina maquina = buscarMaquina(indiceMaquina);
        EstadoMaquina estado = maquina.estado();
        if (estado != EstadoMaquina.OPERANDO && estado != EstadoMaquina.PARADA_MANUAL) {
            throw new AccionNoPermitidaException("No se puede parar ni arrancar "
                    + maquina.etiqueta() + " porque está " + estado.descripcion().toLowerCase());
        }
        planta.ejecutar(new PararOArrancarMaquina(maquina));
        return estadoPlanta();
    }

    /**
     * Cambia la carga de una máquina. Se apila para poder deshacerla.
     *
     * <p>Si la carga nueva es igual a la actual no se hace nada, como en la Entrega 1: apilar una
     * acción que no cambia nada solo ensuciaría la pila de deshacer.</p>
     *
     * @param indiceMaquina posición de la máquina en el inventario
     * @param carga         porcentaje nuevo, entre 0 y 100
     * @return el estado de la planta después de la acción
     * @throws NoEncontradoException    si la máquina no existe
     * @throws IllegalArgumentException si la carga está fuera de rango
     */
    public synchronized EstadoPlantaDto cambiarCarga(int indiceMaquina, int carga) {
        Maquina maquina = buscarMaquina(indiceMaquina);
        CambiarCargaMaquina comando = new CambiarCargaMaquina(maquina, carga);
        if (maquina.carga() != carga) {
            planta.ejecutar(comando);
        }
        return estadoPlanta();
    }

    /**
     * Revierte la última acción del operador, la de la cima de la pila.
     *
     * <p>Si el comando ya no se puede revertir porque la simulación cambió el estado, el modelo lo
     * descarta y lo anota en la bitácora; eso es un caso normal, no un error. Con la pila vacía
     * el modelo también lo anota, y después se lanza la excepción.</p>
     *
     * @return el estado de la planta después de la acción
     * @throws AccionNoPermitidaException si no hay acciones para deshacer
     */
    public synchronized EstadoPlantaDto deshacer() {
        boolean pilaVacia = planta.accionesDeshacibles().isEmpty();
        planta.deshacer();
        if (pilaVacia) {
            throw new AccionNoPermitidaException("No hay acciones para deshacer");
        }
        return estadoPlanta();
    }

    private Maquina buscarMaquina(int indice) {
        if (indice < 0 || indice >= planta.inventario().size()) {
            throw new NoEncontradoException("No existe una máquina con índice " + indice);
        }
        return planta.maquina(indice);
    }

    private static Magnitud convertirMagnitud(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Falta la magnitud: LEVE, MODERADA o SEVERA");
        }
        try {
            return Magnitud.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La magnitud '" + texto
                    + "' no existe: use LEVE, MODERADA o SEVERA");
        }
    }

    // ---------------------------------------------------------------------------------
    // Reloj y consultas
    // ---------------------------------------------------------------------------------

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
     * Toma una foto del estado completo de la planta.
     *
     * <p>La foto se arma <strong>dentro</strong> de este método {@code synchronized}: mientras se
     * copia, ni el reloj ni otra petición pueden cambiar la planta, así que nunca se entrega un tick
     * a medias. Lo que sale de aquí es una copia; la planta no se entrega nunca.</p>
     *
     * <p>Complejidad: O(n) en el tamaño total del estado.</p>
     *
     * @return la foto del estado, inmutable
     */
    public synchronized EstadoPlantaDto estadoPlanta() {
        return EstadoPlantaDto.desde(planta, enEjecucion);
    }

    /**
     * Devuelve el tick, la semilla y si la simulación está en ejecución.
     *
     * <p>Las acciones de control devuelven este mismo DTO, armado dentro de su propio método
     * {@code synchronized}: así la respuesta refleja exactamente el efecto de la acción, sin que
     * un tick del reloj se cuele entre la acción y la consulta.</p>
     *
     * @return el estado de la simulación
     */
    public synchronized EstadoSimulacionDto estadoSimulacion() {
        return new EstadoSimulacionDto(planta.tickActual(), planta.semilla(), enEjecucion);
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
