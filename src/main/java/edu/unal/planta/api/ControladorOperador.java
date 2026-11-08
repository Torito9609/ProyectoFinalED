package edu.unal.planta.api;

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
 * Endpoints de las acciones del operador sobre la planta.
 *
 * <p>Cada uno llama una sola vez al servicio y devuelve el estado completo de la planta, para que
 * el frontend vuelva a dibujar. Las validaciones y los errores ({@code 400}, {@code 404},
 * {@code 409}) los deciden el servicio y {@link ManejadorErrores}, no esta clase.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestController
@RequestMapping("/api/planta")
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
     * {@code POST /api/planta/maquinas/{indice}/inyectar-fallo}: provoca una anomalía.
     *
     * @param indice  posición de la máquina en el inventario
     * @param cuerpo  sensor y magnitud del fallo
     * @return el estado de la planta después de la acción
     */
    @PostMapping("/maquinas/{indice}/inyectar-fallo")
    public EstadoPlantaDto inyectarFallo(@PathVariable int indice, @RequestBody InyeccionFalloDto cuerpo) {
        return servicio.inyectarFallo(indice, cuerpo.indiceSensor(), cuerpo.magnitud());
    }

    /**
     * {@code POST /api/planta/maquinas/{indice}/parar-o-arrancar}: alterna entre operando y parada
     * manual. Se puede deshacer.
     *
     * @param indice posición de la máquina en el inventario
     * @return el estado de la planta después de la acción
     */
    @PostMapping("/maquinas/{indice}/parar-o-arrancar")
    public EstadoPlantaDto pararOArrancar(@PathVariable int indice) {
        return servicio.pararOArrancar(indice);
    }

    /**
     * {@code PUT /api/planta/maquinas/{indice}/carga}: cambia la carga. Se puede deshacer.
     *
     * @param indice posición de la máquina en el inventario
     * @param cuerpo la carga nueva
     * @return el estado de la planta después de la acción
     */
    @PutMapping("/maquinas/{indice}/carga")
    public EstadoPlantaDto cambiarCarga(@PathVariable int indice, @RequestBody CargaDto cuerpo) {
        return servicio.cambiarCarga(indice, cuerpo.carga());
    }

    /**
     * {@code POST /api/planta/deshacer}: revierte la última acción del operador (cima de la pila).
     *
     * @return el estado de la planta después de la acción
     */
    @PostMapping("/deshacer")
    public EstadoPlantaDto deshacer() {
        return servicio.deshacer();
    }
}
