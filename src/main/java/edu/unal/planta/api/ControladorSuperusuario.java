package edu.unal.planta.api;

import edu.unal.planta.api.dto.RankingDto;
import edu.unal.planta.servicio.ServicioSimulacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la vista del superusuario: el ranking de fallos con sus recomendaciones. Aquí
 * entrarán también los overhauls y el historial.
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
}
