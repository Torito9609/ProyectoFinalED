package edu.unal.planta.api;

import edu.unal.planta.api.dto.AprobarOverhaulDto;
import edu.unal.planta.api.dto.EstadoOverhaulsDto;
import edu.unal.planta.api.dto.RankingDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la vista del superusuario: el ranking de fallos con sus recomendaciones y las
 * solicitudes de overhaul con los técnicos del fabricante. Aquí entrará también el historial.
 *
 * <p>Como los demás controladores, cada método llama una sola vez al servicio y devuelve el DTO
 * que este arma.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestController
@RequestMapping("/api")
public class ControladorSuperusuario {

    private final ServicioSimulacion servicio;

    /**
     * Crea el controlador. Spring le pasa el servicio, que es único en la aplicación.
     *
     * @param servicio el servicio de simulación
     */
    public ControladorSuperusuario(ServicioSimulacion servicio) {
        this.servicio = servicio;
    }

    /**
     * {@code GET /api/ranking}: el ranking de fallos, global o de una compañía.
     *
     * @param compania       id de la compañía; si no viene, el ranking global
     * @param puntajeMinimo  si viene, solo las máquinas con ese puntaje o más (consulta por rango)
     * @return el ranking
     */
    @GetMapping("/ranking")
    public RankingDto ranking(@RequestParam(required = false) Integer compania,
                              @RequestParam(defaultValue = "0") int puntajeMinimo) {
        return servicio.ranking(compania, puntajeMinimo);
    }

    /**
     * {@code GET /api/overhauls}: el montículo de solicitudes (su cima y su orden de salida) y los
     * técnicos del fabricante.
     *
     * @return la foto del montículo y de los técnicos
     */
    @GetMapping("/overhauls")
    public EstadoOverhaulsDto overhauls() {
        return servicio.estadoOverhauls();
    }

    /**
     * {@code POST /api/overhauls}: aprueba un overhaul para una máquina con recomendación.
     *
     * @param cuerpo la compañía y el índice de la máquina
     * @return la foto del montículo después de la acción
     */
    @PostMapping("/overhauls")
    public EstadoOverhaulsDto aprobarOverhaul(@RequestBody AprobarOverhaulDto cuerpo) {
        return servicio.aprobarOverhaul(cuerpo.idCompania(), cuerpo.indiceMaquina());
    }

    /**
     * {@code POST /api/overhauls/asignar-siguiente}: botón del modo manual; despacha el camión del
     * fabricante para la solicitud de la cima.
     *
     * @return la foto del montículo después de la acción
     */
    @PostMapping("/overhauls/asignar-siguiente")
    public EstadoOverhaulsDto asignarSiguienteOverhaul() {
        return servicio.asignarSiguienteOverhaul();
    }
}
