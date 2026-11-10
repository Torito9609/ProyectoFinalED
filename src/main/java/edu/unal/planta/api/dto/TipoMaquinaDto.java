package edu.unal.planta.api.dto;

import edu.unal.planta.modelo.TipoMaquina;
import java.util.Arrays;
import java.util.List;

/**
 * Un tipo del catálogo de máquinas, para el formulario de crear máquina del frontend.
 *
 * <p>El catálogo vive en un solo lugar, el enumerado {@link TipoMaquina}: el frontend lo pide en
 * vez de copiarlo.</p>
 *
 * @param tipo        código del tipo, el que se envía al crear una máquina
 * @param descripcion nombre para mostrar
 * @param prefijo     prefijo de las etiquetas de este tipo, por ejemplo {@code COMPRES} para
 *                    {@code COMPRES-01}
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record TipoMaquinaDto(String tipo, String descripcion, String prefijo) {

    /**
     * Copia todo el catálogo, en el orden del enumerado.
     *
     * <p>Complejidad: O(t), con t el número de tipos.</p>
     *
     * @return lista no modificable con un elemento por tipo
     */
    public static List<TipoMaquinaDto> catalogo() {
        return Arrays.stream(TipoMaquina.values())
                .map(tipo -> new TipoMaquinaDto(tipo.name(), tipo.descripcion(), tipo.prefijo()))
                .toList();
    }
}
