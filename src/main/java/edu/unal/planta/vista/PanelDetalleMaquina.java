package edu.unal.planta.vista;

import edu.unal.planta.modelo.Anomalia;
import edu.unal.planta.modelo.Lectura;
import edu.unal.planta.modelo.Maquina;
import edu.unal.planta.modelo.Sensor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * Muestra el detalle de la máquina seleccionada: su estado, su desgaste y la última lectura de cada
 * sensor comparada con sus umbrales.
 *
 * <p>Es el panel que permite entender <em>por qué</em> se disparó una alarma: se ve el valor, se ve
 * en qué zona cayó y se ve cuál es el sensor que tiene la anomalía.</p>
 *
 * <p>En esta etapa el panel solo muestra información. Los controles para cambiar la carga, parar o
 * arrancar la máquina e inyectar un fallo se agregan junto con el controlador, porque toda acción
 * del usuario debe pasar por él.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class PanelDetalleMaquina extends PanelEstructura {

    private static final int ALTO_SENSOR = 30;

    /** Prepara el panel. */
    public PanelDetalleMaquina() {
        super("DETALLE DE LA MÁQUINA");
        setPreferredSize(new Dimension(250, 190));
    }

    @Override
    protected String subtitulo() {
        return planta() == null ? "" : "índice " + seleccion().indiceMaquina();
    }

    @Override
    protected void dibujarContenido(Graphics2D g, Rectangle area) {
        Maquina maquina = planta().maquina(seleccion().indiceMaquina());

        g.setFont(Colores.FUENTE_FUERTE);
        g.setColor(Colores.TEXTO);
        g.drawString(maquina.etiqueta(), area.x, area.y + 11);

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.TEXTO_SUAVE);
        g.drawString(maquina.tipo(), area.x, area.y + 22);

        Color estado = Colores.de(maquina.estado().rgb());
        g.setFont(Colores.FUENTE_ETIQUETA);
        g.setColor(estado.darker());
        g.drawString(maquina.estado().descripcion(), area.x, area.y + 36);

        g.setFont(Colores.FUENTE_PEQUENA);
        g.setColor(Colores.TEXTO_SUAVE);
        g.drawString("carga " + maquina.carga() + "%   ·   desgaste "
                + maquina.ticksDesdeMantenimiento() + "/" + maquina.intervaloPreventivo(),
                area.x, area.y + 48);

        dibujarAnomalia(g, area, maquina);
        dibujarSensores(g, area, maquina);
    }

    /**
     * Indica si la máquina tiene un fallo físico en curso y en qué sensor.
     *
     * @param g       lienzo
     * @param area    zona disponible
     * @param maquina máquina seleccionada
     */
    private void dibujarAnomalia(Graphics2D g, Rectangle area, Maquina maquina) {
        Optional<Anomalia> anomalia = maquina.anomalia();
        g.setFont(Colores.FUENTE_PEQUENA);
        if (anomalia.isEmpty()) {
            g.setColor(Colores.ZONA_NORMAL);
            g.drawString("sin anomalía", area.x, area.y + 60);
            return;
        }
        Anomalia fallo = anomalia.orElseThrow();
        g.setColor(Colores.de(edu.unal.planta.modelo.Prioridad.P1.rgb()));
        g.drawString("anomalía " + fallo.magnitud().descripcion().toLowerCase() + " en "
                + maquina.sensor(fallo.indiceSensor()).nombre(), area.x, area.y + 60);
    }

    /**
     * Dibuja cada sensor con su última lectura y una barra que ubica el valor entre los umbrales.
     *
     * @param g       lienzo
     * @param area    zona disponible
     * @param maquina máquina seleccionada
     */
    private void dibujarSensores(Graphics2D g, Rectangle area, Maquina maquina) {
        Optional<Lectura> ultima = maquina.lecturas().estaVacia()
                ? Optional.empty()
                : Optional.of(maquina.lecturas().verMasReciente());

        for (int indice = 0; indice < maquina.sensores().size(); indice++) {
            Sensor sensor = maquina.sensor(indice);
            int y = area.y + 74 + indice * ALTO_SENSOR;
            boolean seleccionado = indice == seleccion().indiceSensor();

            g.setFont(seleccionado ? Colores.FUENTE_FUERTE : Colores.FUENTE_ETIQUETA);
            g.setColor(Colores.TEXTO);
            g.drawString(sensor.nombre(), area.x, y);

            if (ultima.isEmpty()) {
                g.setFont(Colores.FUENTE_PEQUENA);
                g.setColor(Colores.TEXTO_SUAVE);
                g.drawString("sin lecturas", area.x, y + 11);
                continue;
            }
            double valor = ultima.orElseThrow().valor(indice);
            Color zona = Colores.zonaDe(sensor, valor);

            g.setFont(Colores.FUENTE_VALOR);
            g.setColor(zona.darker());
            String texto = sensor.formatear(valor);
            g.drawString(texto, area.x + area.width - g.getFontMetrics().stringWidth(texto) - 2, y);

            dibujarBarra(g, new Rectangle(area.x, y + 5, area.width - 2, 6), sensor, valor, zona);
        }
    }

    /**
     * Dibuja una barra con las tres zonas de umbrales y una marca en el valor actual.
     *
     * @param g      lienzo
     * @param barra  posición y tamaño
     * @param sensor sensor que se muestra
     * @param valor  valor leído
     * @param zona   color de la zona en que cayó el valor
     */
    private void dibujarBarra(Graphics2D g, Rectangle barra, Sensor sensor, double valor,
                              Color zona) {
        double minimo = sensor.nominal();
        double maximo = sensor.umbralCritico() + (sensor.umbralCritico() - sensor.umbralAlto());
        double rango = Math.max(1e-6, maximo - minimo);

        g.setColor(Colores.CELDA_VACIA);
        g.fillRect(barra.x, barra.y, barra.width, barra.height);

        marcarUmbral(g, barra, sensor.umbralAdvertencia(), minimo, rango,
                Colores.de(edu.unal.planta.modelo.Prioridad.P3.rgb()));
        marcarUmbral(g, barra, sensor.umbralAlto(), minimo, rango,
                Colores.de(edu.unal.planta.modelo.Prioridad.P2.rgb()));
        marcarUmbral(g, barra, sensor.umbralCritico(), minimo, rango,
                Colores.de(edu.unal.planta.modelo.Prioridad.P1.rgb()));

        int x = barra.x + (int) (barra.width * clamp((valor - minimo) / rango));
        g.setColor(zona);
        g.fillRect(Math.min(x, barra.x + barra.width - 3), barra.y - 2, 3, barra.height + 4);
    }

    /**
     * Pinta una marca vertical en la posición de un umbral.
     *
     * @param g      lienzo
     * @param barra  barra donde se marca
     * @param umbral valor del umbral
     * @param minimo valor correspondiente al extremo izquierdo
     * @param rango  ancho de la escala
     * @param color  color del umbral
     */
    private void marcarUmbral(Graphics2D g, Rectangle barra, double umbral, double minimo,
                              double rango, Color color) {
        int x = barra.x + (int) (barra.width * clamp((umbral - minimo) / rango));
        g.setColor(Colores.aclarar(color, 0.4));
        g.fillRect(x, barra.y, Math.max(1, barra.x + barra.width - x), barra.height);
    }

    /**
     * Acota una proporción al intervalo de 0 a 1.
     *
     * @param valor proporción calculada
     * @return la proporción dentro del intervalo válido
     */
    private static double clamp(double valor) {
        return Math.max(0, Math.min(1, valor));
    }
}
