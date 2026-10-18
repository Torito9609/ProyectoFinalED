package edu.unal.planta.vista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import edu.unal.planta.modelo.Planta;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba de los paneles de la vista.
 *
 * <p>Es una sola, porque los paneles son visuales y comprobar píxeles con JUnit no aportaría nada:
 * el aspecto se revisa mirándolo. Lo que sí vale la pena asegurar es que <strong>ninguno falle al
 * dibujarse</strong>, y en particular con una planta recién creada, donde las máquinas todavía no
 * tienen ninguna lectura.</p>
 *
 * <p>Ese es el caso borde que rompería la aplicación justo al arrancar, antes del primer tick: un
 * panel que pidiera la última lectura de una cola vacía recibiría una excepción y la ventana
 * aparecería en blanco.</p>
 */
@DisplayName("Paneles de la vista")
class PanelesVistaTest {

    @Test
    @DisplayName("todos se dibujan sin fallar, con la planta recién creada y con la simulación en marcha")
    void todosLosPanelesSeDibujan() {
        Planta reciencreada = new Planta(7L);
        Planta enMarcha = new Planta(7L);
        for (int i = 0; i < 120; i++) {
            enMarcha.tick();
        }

        for (Planta planta : List.of(reciencreada, enMarcha)) {
            for (int indiceMaquina = 0; indiceMaquina < planta.inventario().size(); indiceMaquina++) {
                SeleccionVista seleccion = new SeleccionVista(indiceMaquina, 0);
                for (PanelEstructura panel : crearPaneles()) {
                    assertDoesNotThrow(() -> dibujar(panel, planta, seleccion),
                            () -> panel.getClass().getSimpleName()
                                    + " falló con la máquina " + seleccion.indiceMaquina()
                                    + " en el tick " + planta.tickActual());
                }
            }
        }
    }

    /**
     * Crea un ejemplar de cada panel de la ventana.
     *
     * @return los paneles a probar
     */
    private static List<PanelEstructura> crearPaneles() {
        return List.of(
                new PanelInventario(),
                new PanelDetalleMaquina(),
                new PanelColaCircular(),
                new PanelColaEventos(),
                new PanelColaTecnicos(),
                new PanelColaPrioridad(),
                new PanelPila(),
                new PanelBitacora());
    }

    /**
     * Refresca un panel y lo pinta sobre una imagen en memoria.
     *
     * <p>Pintar sobre un {@link BufferedImage} funciona sin pantalla, así que la prueba corre
     * igual en una máquina sin entorno gráfico.</p>
     *
     * @param panel     panel a dibujar
     * @param planta    estado a mostrar
     * @param seleccion qué máquina y sensor se están mirando
     */
    private static void dibujar(PanelEstructura panel, Planta planta, SeleccionVista seleccion) {
        panel.refrescar(planta, seleccion);
        panel.setSize(640, 220);
        panel.doLayout();

        BufferedImage lienzo = new BufferedImage(640, 220, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lienzo.createGraphics();
        try {
            panel.paint(g);
        } finally {
            g.dispose();
        }
    }
}
