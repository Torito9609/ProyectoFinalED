package edu.unal.planta.modelo;

import java.util.Comparator;
import java.util.Objects;

/**
 * Solicitud aprobada de overhaul para una máquina, que espera en el montículo a un técnico del
 * fabricante.
 *
 * <h2>Por qué el puntaje se congela</h2>
 * <p>La clave del montículo es el puntaje de la máquina <strong>en el momento de aprobar</strong>,
 * guardado aquí. Si el montículo leyera el puntaje vivo de la máquina, la clave cambiaría con la
 * solicitud adentro, y {@code PriorityQueue} no se reordena sola: quedaría mal ordenado sin lanzar
 * ningún error. Es la misma trampa de la clave que cambia del {@code TreeSet} del ranking.</p>
 *
 * <h2>Por qué hay número de secuencia</h2>
 * <p>{@code PriorityQueue} no garantiza el orden de llegada entre elementos con la misma clave, y
 * por eso se descartó en la Entrega 1. El {@link #COMPARADOR} desempata por la secuencia, que
 * crece con cada aprobación: entre dos solicitudes con el mismo puntaje, sale primero la que llegó
 * antes.</p>
 *
 * @param idCompania       id de la compañía de la máquina
 * @param indiceMaquina    índice de la máquina en su inventario
 * @param etiqueta         etiqueta de la máquina, para mostrarla
 * @param puntajeCongelado puntaje de fallos al aprobar: la clave del montículo
 * @param secuencia        número de llegada, para desempatar
 * @param tickAprobacion   tick en que se aprobó
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record SolicitudOverhaul(
        int idCompania,
        int indiceMaquina,
        String etiqueta,
        int puntajeCongelado,
        long secuencia,
        int tickAprobacion) {

    /**
     * Orden del montículo: mayor puntaje primero; en un empate, la que llegó antes.
     *
     * <p>Con este comparador, la cima del montículo es la máquina en peor estado.</p>
     */
    public static final Comparator<SolicitudOverhaul> COMPARADOR =
            Comparator.comparingInt(SolicitudOverhaul::puntajeCongelado).reversed()
                    .thenComparingLong(SolicitudOverhaul::secuencia);

    /**
     * Valida la solicitud.
     *
     * @throws NullPointerException si la etiqueta es nula
     */
    public SolicitudOverhaul {
        Objects.requireNonNull(etiqueta, "La solicitud debe indicar la máquina");
    }
}
