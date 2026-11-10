package edu.unal.planta.servicio;

import edu.unal.planta.api.dto.EstadoPlantaDto;
import edu.unal.planta.api.dto.EstadoSimulacionDto;
import edu.unal.planta.api.dto.ResumenCompaniaDto;
import edu.unal.planta.modelo.EstadoMaquina;
import edu.unal.planta.modelo.Magnitud;
import edu.unal.planta.modelo.Maquina;
import edu.unal.planta.modelo.Organizacion;
import edu.unal.planta.modelo.Planta;
import edu.unal.planta.modelo.comandos.CambiarCargaMaquina;
import edu.unal.planta.modelo.comandos.PararOArrancarMaquina;
import java.util.List;
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
 * corre en otro. Si dos hilos tocaran el modelo a la vez, una petición podría ver un tick a
 * medias, o dos acciones podrían mezclarse.</p>
 *
 * <p>La solución es la regla 4 de la sección 4 de {@code CLAUDE.md}: esta clase es la
 * <strong>única</strong> que guarda la {@link Organizacion} (con todas sus compañías), nunca la
 * entrega hacia afuera y
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
 * <h2>Compañías</h2>
 * <p>El reloj es uno solo para toda la organización: iniciar, pausar, paso y reiniciar actúan
 * sobre todas las compañías. Las acciones del operador y las consultas de estado reciben el id de
 * la compañía, que es su índice en la organización: buscarla cuesta O(1).</p>
 *
 * <p>Todas las operaciones cuestan lo que cuesta la operación del modelo que llaman: O(1) las de
 * control, y lo que cueste un {@link Organizacion#tick()} las que avanzan la simulación.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@Service
public class ServicioSimulacion {

    /** La organización simulada, con todas sus compañías. Solo esta clase la conoce. */
    private final Organizacion organizacion;

    /** {@code true} si el reloj debe avanzar la simulación; {@code false} si está en pausa. */
    private boolean enEjecucion;

    /**
     * Crea el servicio con una organización nueva, en pausa.
     *
     * <p>Spring llama a este constructor y le pasa la semilla de {@code application.properties}.
     * Las pruebas lo llaman directamente, sin Spring.</p>
     *
     * @param semillaMaestra semilla maestra, de la que se derivan las de las compañías; la misma
     *                       semilla repite la simulación
     */
    public ServicioSimulacion(@Value("${planta.semilla}") long semillaMaestra) {
        this.organizacion = new Organizacion(semillaMaestra);
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
     * Pone la simulación en pausa. El estado de las compañías no cambia.
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
        organizacion.tick();
        return estadoSimulacion();
    }

    /**
     * Pausa y devuelve todas las compañías a su estado inicial, con las mismas semillas. Las
     * compañías y máquinas creadas se conservan.
     *
     * @return el estado de la simulación después de la acción
     */
    public synchronized EstadoSimulacionDto reiniciar() {
        enEjecucion = false;
        organizacion.reiniciar();
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
     * @param idCompania    id de la compañía
     * @param indiceMaquina posición de la máquina en el inventario
     * @param indiceSensor  posición del sensor en la máquina
     * @param magnitud      {@code LEVE}, {@code MODERADA} o {@code SEVERA}
     * @return el estado de la compañía después de la acción
     * @throws NoEncontradoException      si la compañía, la máquina o el sensor no existen
     * @throws IllegalArgumentException   si la magnitud no existe
     * @throws AccionNoPermitidaException si la máquina ya tiene un fallo activo
     */
    public synchronized EstadoPlantaDto inyectarFallo(int idCompania, int indiceMaquina,
                                                      int indiceSensor, String magnitud) {
        Planta planta = buscarCompania(idCompania);
        Maquina maquina = buscarMaquina(planta, indiceMaquina);
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
        return EstadoPlantaDto.desde(planta, enEjecucion);
    }

    /**
     * Para una máquina que está operando, o arranca una que está parada a mano. Se apila para
     * poder deshacerla.
     *
     * @param idCompania    id de la compañía
     * @param indiceMaquina posición de la máquina en el inventario
     * @return el estado de la compañía después de la acción
     * @throws NoEncontradoException      si la compañía o la máquina no existen
     * @throws AccionNoPermitidaException si la máquina está en otro estado, porque entonces la
     *                                    está atendiendo la simulación
     */
    public synchronized EstadoPlantaDto pararOArrancar(int idCompania, int indiceMaquina) {
        Planta planta = buscarCompania(idCompania);
        Maquina maquina = buscarMaquina(planta, indiceMaquina);
        EstadoMaquina estado = maquina.estado();
        if (estado != EstadoMaquina.OPERANDO && estado != EstadoMaquina.PARADA_MANUAL) {
            throw new AccionNoPermitidaException("No se puede parar ni arrancar "
                    + maquina.etiqueta() + " porque está " + estado.descripcion().toLowerCase());
        }
        planta.ejecutar(new PararOArrancarMaquina(maquina));
        return EstadoPlantaDto.desde(planta, enEjecucion);
    }

    /**
     * Cambia la carga de una máquina. Se apila para poder deshacerla.
     *
     * <p>Si la carga nueva es igual a la actual no se hace nada, como en la Entrega 1: apilar una
     * acción que no cambia nada solo ensuciaría la pila de deshacer.</p>
     *
     * @param idCompania    id de la compañía
     * @param indiceMaquina posición de la máquina en el inventario
     * @param carga         porcentaje nuevo, entre 0 y 100
     * @return el estado de la compañía después de la acción
     * @throws NoEncontradoException    si la compañía o la máquina no existen
     * @throws IllegalArgumentException si la carga está fuera de rango
     */
    public synchronized EstadoPlantaDto cambiarCarga(int idCompania, int indiceMaquina, int carga) {
        Planta planta = buscarCompania(idCompania);
        Maquina maquina = buscarMaquina(planta, indiceMaquina);
        CambiarCargaMaquina comando = new CambiarCargaMaquina(maquina, carga);
        if (maquina.carga() != carga) {
            planta.ejecutar(comando);
        }
        return EstadoPlantaDto.desde(planta, enEjecucion);
    }

    /**
     * Revierte la última acción del operador en una compañía, la de la cima de su pila.
     *
     * <p>Si el comando ya no se puede revertir porque la simulación cambió el estado, el modelo lo
     * descarta y lo anota en la bitácora; eso es un caso normal, no un error. Con la pila vacía
     * el modelo también lo anota, y después se lanza la excepción.</p>
     *
     * @param idCompania id de la compañía
     * @return el estado de la compañía después de la acción
     * @throws NoEncontradoException      si la compañía no existe
     * @throws AccionNoPermitidaException si no hay acciones para deshacer
     */
    public synchronized EstadoPlantaDto deshacer(int idCompania) {
        Planta planta = buscarCompania(idCompania);
        boolean pilaVacia = planta.accionesDeshacibles().isEmpty();
        planta.deshacer();
        if (pilaVacia) {
            throw new AccionNoPermitidaException("No hay acciones para deshacer");
        }
        return EstadoPlantaDto.desde(planta, enEjecucion);
    }

    private Planta buscarCompania(int id) {
        if (id < 0 || id >= organizacion.companias().size()) {
            throw new NoEncontradoException("No existe una compañía con id " + id);
        }
        return organizacion.compania(id);
    }

    private static Maquina buscarMaquina(Planta planta, int indice) {
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
            organizacion.tick();
        }
    }

    /**
     * Devuelve los totales de cada compañía, en el orden de sus ids.
     *
     * <p>Complejidad: O(N), con N el total de máquinas: se recorre cada inventario para contar los
     * estados.</p>
     *
     * @return un resumen por compañía, inmutable
     */
    public synchronized List<ResumenCompaniaDto> resumenCompanias() {
        return organizacion.companias().stream().map(ResumenCompaniaDto::desde).toList();
    }

    /**
     * Toma una foto del estado completo de una compañía.
     *
     * <p>La foto se arma <strong>dentro</strong> de este método {@code synchronized}: mientras se
     * copia, ni el reloj ni otra petición pueden cambiar la planta, así que nunca se entrega un tick
     * a medias. Lo que sale de aquí es una copia; la planta no se entrega nunca.</p>
     *
     * <p>Complejidad: O(1) para encontrar la compañía, y O(n) en el tamaño de su estado.</p>
     *
     * @param idCompania id de la compañía
     * @return la foto del estado, inmutable
     * @throws NoEncontradoException si la compañía no existe
     */
    public synchronized EstadoPlantaDto estadoCompania(int idCompania) {
        return EstadoPlantaDto.desde(buscarCompania(idCompania), enEjecucion);
    }

    /**
     * Devuelve el tick, la semilla maestra y si la simulación está en ejecución.
     *
     * <p>Las acciones de control devuelven este mismo DTO, armado dentro de su propio método
     * {@code synchronized}: así la respuesta refleja exactamente el efecto de la acción, sin que
     * un tick del reloj se cuele entre la acción y la consulta.</p>
     *
     * @return el estado de la simulación
     */
    public synchronized EstadoSimulacionDto estadoSimulacion() {
        return new EstadoSimulacionDto(organizacion.tickActual(), organizacion.semillaMaestra(),
                enEjecucion);
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
        return organizacion.tickActual();
    }

    /**
     * Devuelve la semilla maestra con que corre la simulación.
     *
     * @return semilla de la que se derivan las de las compañías
     */
    public synchronized long semilla() {
        return organizacion.semillaMaestra();
    }
}
