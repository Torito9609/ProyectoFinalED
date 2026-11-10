package edu.unal.planta.api;

import edu.unal.planta.api.dto.CrearCompaniaDto;
import edu.unal.planta.api.dto.CrearMaquinaDto;
import edu.unal.planta.api.dto.EstadoPlantaDto;
import edu.unal.planta.api.dto.ResumenCompaniaDto;
import edu.unal.planta.api.dto.TipoMaquinaDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de las compañías: consultarlas, crearlas, crear máquinas en ellas y el catálogo de
 * tipos de máquina.
 *
 * <p>Como los demás controladores, cada método llama una sola vez al servicio y devuelve el DTO
 * que este arma. El id de una compañía es su índice en la organización. Crear responde
 * {@code 201}; los errores ({@code 400}, {@code 404}, {@code 409}) los decide el servicio.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestController
@RequestMapping("/api")
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
    @GetMapping("/companias")
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
    @GetMapping("/companias/{id}")
    public EstadoPlantaDto compania(@PathVariable int id) {
        return servicio.estadoCompania(id);
    }

    /**
     * {@code POST /api/companias}: crea una compañía al final de la organización.
     *
     * @param cuerpo el nombre de la compañía
     * @return los totales de la compañía creada, con su id
     */
    @PostMapping("/companias")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumenCompaniaDto crearCompania(@RequestBody CrearCompaniaDto cuerpo) {
        return servicio.crearCompania(cuerpo.nombre());
    }

    /**
     * {@code POST /api/companias/{id}/maquinas}: crea una máquina al final del inventario de una
     * compañía.
     *
     * @param id     id de la compañía
     * @param cuerpo tipo y etiqueta de la máquina
     * @return el estado de la compañía con la máquina nueva
     */
    @PostMapping("/companias/{id}/maquinas")
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoPlantaDto crearMaquina(@PathVariable int id, @RequestBody CrearMaquinaDto cuerpo) {
        return servicio.crearMaquina(id, cuerpo.tipo(), cuerpo.etiqueta());
    }

    /**
     * {@code GET /api/tipos-maquina}: el catálogo de tipos, para el formulario de crear máquina.
     *
     * @return un elemento por tipo
     */
    @GetMapping("/tipos-maquina")
    public List<TipoMaquinaDto> tiposMaquina() {
        return servicio.tiposMaquina();
    }
}
