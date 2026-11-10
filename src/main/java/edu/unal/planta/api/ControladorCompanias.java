package edu.unal.planta.api;

import edu.unal.planta.api.dto.EstadoPlantaDto;
import edu.unal.planta.api.dto.ResumenCompaniaDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consulta de las compañías: la lista con sus totales y el estado completo de una.
 *
 * <p>Como los demás controladores, cada método llama una sola vez al servicio y devuelve el DTO
 * que este arma. El id de una compañía es su índice en la organización.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestController
@RequestMapping("/api/companias")
public class ControladorCompanias {

    private final ServicioSimulacion servicio;

    /**
     * Crea el controlador. Spring le pasa el servicio, que es único en la aplicación.
     *
     * @param servicio el servicio de simulación
     */
    public ControladorCompanias(ServicioSimulacion servicio) {
        this.servicio = servicio;
    }

    /**
     * {@code GET /api/companias}: los totales de cada compañía, en el orden de sus ids.
     *
     * @return un resumen por compañía
     */
    @GetMapping
    public List<ResumenCompaniaDto> companias() {
        return servicio.resumenCompanias();
    }

    /**
     * {@code GET /api/companias/{id}}: el estado completo de una compañía, para dibujar todos los
     * paneles.
     *
     * @param id id de la compañía
     * @return la foto del estado
     */
    @GetMapping("/{id}")
    public EstadoPlantaDto compania(@PathVariable int id) {
        return servicio.estadoCompania(id);
    }
}
