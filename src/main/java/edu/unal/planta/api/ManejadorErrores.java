package edu.unal.planta.api;

import edu.unal.planta.api.dto.ErrorDto;
import edu.unal.planta.servicio.AccionNoPermitidaException;
import edu.unal.planta.servicio.NoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte las excepciones en respuestas HTTP con un {@link ErrorDto}.
 *
 * <ul>
 *   <li>{@code 400}: la entrada es inválida (una carga fuera de rango, una magnitud que no existe,
 *       un JSON mal formado o un índice que no es un número).</li>
 *   <li>{@code 404}: la máquina o el sensor no existe.</li>
 *   <li>{@code 409}: la acción no se permite en el estado actual.</li>
 * </ul>
 *
 * <p>Así los controladores no tienen {@code try}/{@code catch}: el servicio lanza la excepción y
 * esta clase decide el código.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@RestControllerAdvice
public class ManejadorErrores {

    /**
     * Constructor público, porque Spring crea esta clase. No se debe llamar a mano.
     */
    public ManejadorErrores() {
    }

    /**
     * {@code 404}: lo pedido no existe.
     *
     * @param error la excepción lanzada por el servicio
     * @return la respuesta con el mensaje
     */
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorDto> noEncontrado(NoEncontradoException error) {
        return responder(HttpStatus.NOT_FOUND, error.getMessage());
    }

    /**
     * {@code 409}: la acción no se permite en el estado actual.
     *
     * @param error la excepción lanzada por el servicio
     * @return la respuesta con el mensaje
     */
    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorDto> noPermitida(AccionNoPermitidaException error) {
        return responder(HttpStatus.CONFLICT, error.getMessage());
    }

    /**
     * {@code 400}: un valor fuera de rango o que no existe, detectado por el servicio o el modelo.
     *
     * @param error la excepción
     * @return la respuesta con el mensaje
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> entradaInvalida(IllegalArgumentException error) {
        return responder(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    /**
     * {@code 400}: el cuerpo no es un JSON válido o no tiene la forma esperada.
     *
     * @param error la excepción de Spring
     * @return la respuesta con el mensaje
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> cuerpoInvalido(HttpMessageNotReadableException error) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    /**
     * {@code 400}: un parámetro de la ruta no tiene el tipo esperado, por ejemplo un índice que no
     * es un número.
     *
     * @param error la excepción de Spring
     * @return la respuesta con el mensaje
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> parametroInvalido(MethodArgumentTypeMismatchException error) {
        return responder(HttpStatus.BAD_REQUEST, "El parámetro '" + error.getName() + "' no es válido");
    }

    private static ResponseEntity<ErrorDto> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorDto(estado.value(), mensaje));
    }
}
