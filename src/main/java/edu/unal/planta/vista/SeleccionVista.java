package edu.unal.planta.vista;

/**
 * Qué está mirando el operador: cuál máquina del inventario y cuál de sus sensores.
 *
 * <p>Es estado de la <strong>vista</strong>, no del modelo: la planta funciona igual sin importar
 * qué celda tenga seleccionada el usuario, así que este dato no tiene por qué vivir en
 * {@code Planta}. Lo guarda la ventana y lo reparte a los paneles que lo necesitan, que son el de
 * inventario, el de la cola circular y el de detalle.</p>
 *
 * @param indiceMaquina posición de la máquina seleccionada en el inventario
 * @param indiceSensor  posición del sensor seleccionado dentro de esa máquina
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public record SeleccionVista(int indiceMaquina, int indiceSensor) {

    /** Selección inicial: la primera máquina y su primer sensor. */
    public static final SeleccionVista INICIAL = new SeleccionVista(0, 0);

    /**
     * Devuelve la misma selección con otra máquina, volviendo al primer sensor.
     *
     * <p>Se reinicia el sensor porque cada máquina tiene los suyos, y el índice de una no
     * necesariamente corresponde a algo en la otra.</p>
     *
     * @param indice posición de la máquina
     * @return una selección nueva
     */
    public SeleccionVista conMaquina(int indice) {
        return new SeleccionVista(indice, 0);
    }

    /**
     * Devuelve la misma selección con otro sensor.
     *
     * @param indice posición del sensor
     * @return una selección nueva
     */
    public SeleccionVista conSensor(int indice) {
        return new SeleccionVista(indiceMaquina, indice);
    }
}
