package edu.unal.planta.api;

import edu.unal.planta.api.dto.AsignacionDto;
import edu.unal.planta.api.dto.CargaDto;
import edu.unal.planta.api.dto.EstadoPlantaDto;
import edu.unal.planta.api.dto.InyeccionFalloDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de las acciones del operador sobre la planta de una compañía.
 *
 * <p>Cada uno llama una sola vez al servicio y devuelve el estado completo de la compañía, para que
 * el frontend vuelva a dibujar. Las validaciones y los errores ({@code 400}, {@code 404},
 * {@code 409}) los deciden el servicio y {@link ManejadorErrores}, no esta clase.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestController
@RequestMapping("/api/companias/{id}")
public class ControladorOperador {

    private final ServicioSimulacion servicio;

    /**
     * Crea el controlador. Spring le pasa el servicio, que es único en la aplicación.
     *
     * @param servicio el servicio de simulación
     */
    public ControladorOperador(ServicioSimulacion servicio) {
        this.servicio = servicio;
    }

    /**
     * {@code POST /api/companias/{id}/maquinas/{indice}/inyectar-fallo}: provoca una anomalía.
     *
     * @param id      id de la compañía
     * @param indice  posición de la máquina en el inventario
     * @param cuerpo  sensor y magnitud del fallo
     * @return el estado de la compañía después de la acción
     */
    @PostMapping("/maquinas/{indice}/inyectar-fallo")
    public EstadoPlantaDto inyectarFallo(@PathVariable int id, @PathVariable int indice,
                                         @RequestBody InyeccionFalloDto cuerpo) {
        return servicio.inyectarFallo(id, indice, cuerpo.indiceSensor(), cuerpo.magnitud());
    }

    /**
     * {@code POST /api/companias/{id}/maquinas/{indice}/parar-o-arrancar}: alterna entre operando y parada
     * manual. Se puede deshacer.
     *
     * @param id     id de la compañía
     * @param indice posición de la máquina en el inventario
     * @return el estado de la compañía después de la acción
     */
    @PostMapping("/maquinas/{indice}/parar-o-arrancar")
    public EstadoPlantaDto pararOArrancar(@PathVariable int id, @PathVariable int indice) {
        return servicio.pararOArrancar(id, indice);
    }

    /**
     * {@code PUT /api/companias/{id}/maquinas/{indice}/carga}: cambia la carga. Se puede deshacer.
     *
     * @param id     id de la compañía
     * @param indice posición de la máquina en el inventario
     * @param cuerpo la carga nueva
     * @return el estado de la compañía después de la acción
     */
    @PutMapping("/maquinas/{indice}/carga")
    public EstadoPlantaDto cambiarCarga(@PathVariable int id, @PathVariable int indice,
                                        @RequestBody CargaDto cuerpo) {
        return servicio.cambiarCarga(id, indice, cuerpo.carga());
    }

    /**
     * {@code POST /api/companias/{id}/deshacer}: revierte la última acción del operador (cima de la pila).
     *
     * @param id id de la compañía
     * @return el estado de la compañía después de la acción
     */
    @PostMapping("/deshacer")
    public EstadoPlantaDto deshacer(@PathVariable int id) {
        return servicio.deshacer(id);
    }

    /**
     * {@code POST /api/companias/{id}/clasificar-siguiente}: botón del modo manual; clasifica el
     * evento del frente de la cola.
     *
     * @param id id de la compañía
     * @return el estado de la compañía después de la acción
     */
    @PostMapping("/clasificar-siguiente")
    public EstadoPlantaDto clasificarSiguiente(@PathVariable int id) {
        return servicio.clasificarSiguiente(id);
    }

    /**
     * {@code POST /api/companias/{id}/asignar-siguiente}: botón del modo manual; asigna la orden más
     * prioritaria a la cola de técnicos que elige el jugador.
     *
     * @param id     id de la compañía
     * @param cuerpo la especialidad: {@code MECANICO} o {@code ELECTRICO}
     * @return el estado de la compañía después de la acción
     */
    @PostMapping("/asignar-siguiente")
    public EstadoPlantaDto asignarSiguiente(@PathVariable int id, @RequestBody AsignacionDto cuerpo) {
        return servicio.asignarSiguiente(id, cuerpo.especialidad());
    }

    /**
     * {@code POST /api/companias/{id}/maquinas/{indice}/reemplazo}: reemplaza la máquina por una
     * nueva en el mismo índice. No se puede deshacer.
     *
     * @param id     id de la compañía
     * @param indice posición de la máquina en el inventario
     * @return el estado de la compañía con la máquina nueva
     */
    @PostMapping("/maquinas/{indice}/reemplazo")
    public EstadoPlantaDto reemplazar(@PathVariable int id, @PathVariable int indice) {
        return servicio.reemplazarMaquina(id, indice);
    }
}
