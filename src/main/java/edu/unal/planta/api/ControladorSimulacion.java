package edu.unal.planta.api;

import edu.unal.planta.api.dto.EstadoSimulacionDto;
import edu.unal.planta.api.dto.ModoDto;
import edu.unal.planta.api.dto.ResumenPartidaDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de control de la simulación. El reloj es uno solo para toda la organización, así que
 * estas rutas no llevan compañía.
 *
 * <p>Cada método hace lo mismo: llama una sola vez al servicio y devuelve el DTO que este arma. No
 * guarda estado ni decide nada; la concurrencia la resuelve el servicio con sus métodos
 * {@code synchronized}.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestController
@RequestMapping("/api")
public class ControladorSimulacion {

    private final ServicioSimulacion servicio;

    /**
     * Crea el controlador. Spring le pasa el servicio, que es único en la aplicación.
     *
     * @param servicio el servicio de simulación
     */
    public ControladorSimulacion(ServicioSimulacion servicio) {
        this.servicio = servicio;
    }

    /**
     * {@code GET /api/simulacion}: tick, semilla maestra, si la simulación está en ejecución, el
     * modo y si la partida terminó.
     *
     * @return el estado de la simulación
     */
    @GetMapping("/simulacion")
    public EstadoSimulacionDto simulacion() {
        return servicio.estadoSimulacion();
    }

    /**
     * {@code POST /api/simulacion/iniciar}: pone la simulación en ejecución.
     *
     * @return el estado de la simulación después de la acción
     */
    @PostMapping("/simulacion/iniciar")
    public EstadoSimulacionDto iniciar() {
        return servicio.iniciar();
    }

    /**
     * {@code POST /api/simulacion/pausar}: pone la simulación en pausa.
     *
     * @return el estado de la simulación después de la acción
     */
    @PostMapping("/simulacion/pausar")
    public EstadoSimulacionDto pausar() {
        return servicio.pausar();
    }

    /**
     * {@code POST /api/simulacion/paso}: pausa y avanza exactamente un tick.
     *
     * @return el estado de la simulación después de la acción
     */
    @PostMapping("/simulacion/paso")
    public EstadoSimulacionDto paso() {
        return servicio.paso();
    }

    /**
     * {@code POST /api/simulacion/reiniciar}: pausa y vuelve al estado inicial con la misma semilla.
     *
     * @return el estado de la simulación después de la acción
     */
    @PostMapping("/simulacion/reiniciar")
    public EstadoSimulacionDto reiniciar() {
        return servicio.reiniciar();
    }

    /**
     * {@code PUT /api/simulacion/modo}: cambia entre {@code AUTOMATICO} y {@code MANUAL} en todas
     * las compañías, con la simulación en marcha o en pausa.
     *
     * @param cuerpo el modo nuevo
     * @return el estado de la simulación después de la acción
     */
    @PutMapping("/simulacion/modo")
    public EstadoSimulacionDto cambiarModo(@RequestBody ModoDto cuerpo) {
        return servicio.cambiarModo(cuerpo.modo());
    }

    /**
     * {@code GET /api/partida/resumen}: caja, producción, costos, fallos y disponibilidad de cada
     * compañía, y si la partida terminó.
     *
     * @return el resumen de la partida
     */
    @GetMapping("/partida/resumen")
    public ResumenPartidaDto resumenPartida() {
        return servicio.resumenPartida();
    }
}
