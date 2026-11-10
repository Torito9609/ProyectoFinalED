package edu.unal.planta.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unal.planta.modelo.comandos.ComandoOperador;
import edu.unal.planta.modelo.comandos.PararOArrancarMaquina;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la simulación completa, sin interfaz gráfica.
 *
 * <p>Son pocas y cada una cubre una cadena completa de pasos del tick, en vez de probar métodos
 * sueltos. Varias apagan la carga de todas las máquinas para que no aparezcan fallos aleatorios: la
 * probabilidad de fallo es proporcional a la carga, así que con carga en cero lo único que ocurre
 * es lo que la prueba provoca a propósito.</p>
 */
@DisplayName("Planta")
class PlantaTest {

    private static final long SEMILLA = 7L;
    private static final int PRENSA = 0;
    private static final int TORNO = 1;
    private static final int ROBOT = 2;
    private static final int PRESION = 1;
    private static final int CORRIENTE = 1;
    private static final int VIBRACION = 2;

    @Test
    @DisplayName("la misma semilla produce exactamente la misma simulación")
    void mismaSemillaMismaSimulacion() {
        Planta primera = new Planta(SEMILLA);
        Planta segunda = new Planta(SEMILLA);
        Planta distinta = new Planta(SEMILLA + 1);

        for (int i = 0; i < 150; i++) {
            primera.tick();
            segunda.tick();
            distinta.tick();
        }

        assertEquals(primera.bitacora(), segunda.bitacora(),
                "con la misma semilla la simulación debe repetirse igual, para poder ensayar la demo");
        assertNotEquals(primera.bitacora(), distinta.bitacora(),
                "con otra semilla la simulación debe ser distinta");
    }

    @Test
    @DisplayName("tras muchos ticks el ranking coincide con ordenar el inventario, y se rehace al reiniciar")
    void rankingCoincideConOrdenarElInventario() {
        Planta planta = new Planta(SEMILLA);
        for (int i = 0; i < 500; i++) {
            planta.tick();
        }

        List<Maquina> ordenado = new ArrayList<>(planta.inventario());
        ordenado.sort(Comparator.comparingInt(Maquina::puntajeFallos).reversed()
                .thenComparingInt(Maquina::idCompania)
                .thenComparingInt(Maquina::indice));
        assertTrue(ordenado.get(0).puntajeFallos() > 0,
                "la simulación debe haber sumado fallos; si no, la prueba no compara nada");
        assertEquals(ordenado, planta.rankingFallos());

        planta.reiniciar();
        assertEquals(6, planta.rankingFallos().size());
        assertTrue(planta.rankingFallos().stream().allMatch(m -> m.puntajeFallos() == 0));
    }

    @Test
    @DisplayName("una máquina creada queda al final del inventario y en el ranking")
    void crearMaquina() {
        Planta planta = new Planta(SEMILLA);

        Maquina nueva = planta.crearMaquina(TipoMaquina.TORNO, " torno-02 ");

        assertEquals(7, planta.inventario().size());
        assertEquals(nueva, planta.maquina(6));
        assertEquals("TORNO-02", nueva.etiqueta());
        assertEquals(6, nueva.indice());
        assertTrue(planta.rankingFallos().contains(nueva));
        assertTrue(planta.existeEtiqueta("TORNO-02"));
    }

    @Test
    @DisplayName("una etiqueta repetida se rechaza aunque cambien las mayúsculas")
    void etiquetaRepetidaSeRechaza() {
        Planta planta = new Planta(SEMILLA);

        assertTrue(planta.existeEtiqueta("prensa-01"));
        assertThrows(IllegalArgumentException.class,
                () -> planta.crearMaquina(TipoMaquina.PRENSA, "prensa-01"));
        assertEquals(6, planta.inventario().size());
    }

    @Test
    @DisplayName("una anomalía severa termina en una orden P1 asignada a un técnico")
    void anomaliaSeveraProduceP1() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);

        planta.tick();

        Optional<OrdenTrabajo> asignada = ordenEnCurso(planta);
        assertTrue(asignada.isPresent(), "la orden debió asignarse a un técnico");
        assertEquals(Prioridad.P1, asignada.orElseThrow().prioridad(),
                "una lectura sobre el umbral crítico debe clasificarse como P1");
        assertEquals(PRENSA, asignada.orElseThrow().indiceMaquina());
        assertNotEquals(EstadoMaquina.OPERANDO, planta.maquina(PRENSA).estado(),
                "una alarma crítica saca la máquina de operación");
    }

    @Test
    @DisplayName("se clasifica un solo evento por tick, para que la cola FIFO se pueda ver")
    void unEventoPorTick() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.inyectarFallo(TORNO, VIBRACION, Magnitud.SEVERA);

        planta.tick();

        assertEquals(1, planta.eventosPendientes().size(),
                "se detectaron dos eventos y solo debió clasificarse uno");
    }

    @Test
    @DisplayName("cuando el técnico termina, la máquina vuelve a operar sin anomalía")
    void elTecnicoDevuelveLaMaquinaAOperando() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);

        // Un tick para detectar, clasificar y asignar, más la duración de una reparación P1.
        int ticksNecesarios = 1 + ConfiguracionPlanta.duracionReparacion(Prioridad.P1);
        for (int i = 0; i < ticksNecesarios; i++) {
            planta.tick();
        }

        Maquina prensa = planta.maquina(PRENSA);
        assertEquals(EstadoMaquina.OPERANDO, prensa.estado());
        assertFalse(prensa.tieneAnomalia(), "la reparación quita el fallo físico");
        assertFalse(prensa.alarmaActiva(), "y también la alarma");
        assertTrue(prensa.ticksDesdeMantenimiento() > 0,
                "una reparación correctiva no es un servicio preventivo, así que el reloj del "
                        + "preventivo sigue corriendo");
    }

    @Test
    @DisplayName("el preventivo genera una sola orden P4 por máquina, no una por tick")
    void preventivoNoSeRepite() {
        Planta planta = plantaSinFallosAleatorios();
        Maquina prensa = planta.maquina(PRENSA);

        // Suficientes ticks para pasar el intervalo preventivo de la primera máquina.
        for (int i = 0; i < prensa.intervaloPreventivo() + 10; i++) {
            planta.tick();
        }

        // Se cuentan solo las de esta máquina: otras máquinas tienen sus propios intervalos y
        // también cumplen el suyo dentro de esta ventana de ticks.
        long ordenesPreventivas = planta.bitacora().stream()
                .filter(linea -> linea.contains("Preventivo:"))
                .filter(linea -> linea.contains(prensa.etiqueta()))
                .count();

        assertEquals(1, ordenesPreventivas,
                "el indicador de preventiva pendiente debe evitar una orden nueva en cada tick");
    }

    @Test
    @DisplayName("deshacer se descarta si la simulación ya cambió el estado")
    void deshacerSeDescartaSiElEstadoCambio() {
        Planta planta = plantaSinFallosAleatorios();
        Maquina prensa = planta.maquina(PRENSA);

        ComandoOperador parar = new PararOArrancarMaquina(prensa);
        planta.ejecutar(parar);
        assertEquals(EstadoMaquina.PARADA_MANUAL, prensa.estado());

        // La simulación cambia el estado por su cuenta, como lo haría una alarma crítica
        // o un técnico que termina un trabajo.
        prensa.cambiarEstado(EstadoMaquina.DETENIDA);
        assertFalse(parar.sePuedeDeshacer());

        planta.deshacer();

        assertEquals(EstadoMaquina.DETENIDA, prensa.estado(),
                "revertir aquí dejaría un estado inconsistente, así que el comando se descarta");
        assertTrue(planta.bitacora().stream().anyMatch(linea -> linea.contains("Descartado")),
                "el descarte debe quedar registrado en la bitácora");
        assertTrue(planta.accionesDeshacibles().isEmpty(), "el comando salió de la pila");
    }

    @Test
    @DisplayName("un fallo inyectado no reemplaza a otro que aún no se ha leído")
    void inyeccionNoReemplazaAnomaliaPendiente() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.inyectarFallo(PRENSA, PRESION, Magnitud.LEVE);

        planta.tick();

        assertEquals(Prioridad.P1, ordenEnCurso(planta).orElseThrow().prioridad(),
                "la severa no debe desaparecer detrás de la leve");
        assertTrue(planta.bitacora().stream().anyMatch(linea -> linea.contains("rechazada")),
                "el rechazo debe quedar registrado en la bitácora");
    }

    @Test
    @DisplayName("una máquina con alarma activa no acepta otro fallo inyectado")
    void inyeccionRechazadaConAlarmaActiva() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.LEVE);
        planta.tick();
        assertTrue(planta.maquina(PRENSA).alarmaActiva());

        planta.inyectarFallo(PRENSA, PRESION, Magnitud.SEVERA);

        Anomalia anomalia = planta.maquina(PRENSA).anomalia().orElseThrow();
        assertEquals(Magnitud.LEVE, anomalia.magnitud(), "la anomalía original sigue intacta");
        assertEquals(VIBRACION, anomalia.indiceSensor());
    }

    @Test
    @DisplayName("en modo manual no aparecen fallos solos, ni se clasifica ni se asigna nada solo")
    void manualNoPasaNadaSolo() {
        Planta planta = new Planta(SEMILLA);
        planta.cambiarModo(ModoSimulacion.MANUAL);

        for (int i = 0; i < 200; i++) {
            planta.tick();
        }

        assertTrue(planta.inventario().stream().noneMatch(Maquina::tieneAnomalia),
                "en manual los fallos solo se inyectan");
        assertTrue(planta.ordenes().tamano() > 0, "los preventivos siguen generándose solos");
        assertEquals(planta.tecnicos().size(), planta.totalTecnicosLibres(),
                "pero nadie los asigna: todos los técnicos siguen libres");

        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.tick();
        assertEquals(1, planta.eventosPendientes().size(),
                "el evento se detecta solo, pero se queda en la cola hasta que el jugador clasifique");
    }

    @Test
    @DisplayName("en modo manual el jugador clasifica y asigna, y las estructuras deciden qué sale")
    void manualClasificarYAsignar() {
        Planta planta = plantaSinFallosAleatorios();
        planta.cambiarModo(ModoSimulacion.MANUAL);
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.tick();

        assertTrue(planta.clasificarSiguiente());
        assertTrue(planta.eventosPendientes().isEmpty());
        assertEquals(1, planta.ordenes().tamanoNivel(Prioridad.P1));
        assertEquals(EstadoMaquina.DETENIDA, planta.maquina(PRENSA).estado());

        assertTrue(planta.asignarSiguiente(TipoFallo.MECANICO));
        assertEquals(EstadoMaquina.EN_MANTENIMIENTO, planta.maquina(PRENSA).estado());
        assertEquals(PRENSA, ordenEnCurso(planta).orElseThrow().indiceMaquina());

        assertFalse(planta.clasificarSiguiente(), "ya no quedan eventos");
        assertEquals("Tick 1: No hay eventos por clasificar", planta.bitacora().getLast());
    }

    @Test
    @DisplayName("los botones del modo manual se rechazan en modo automático")
    void botonesRechazadosEnAutomatico() {
        Planta planta = new Planta(SEMILLA);

        assertThrows(IllegalStateException.class, planta::clasificarSiguiente);
        assertThrows(IllegalStateException.class, () -> planta.asignarSiguiente(TipoFallo.MECANICO));
    }

    @Test
    @DisplayName("reiniciar conserva el modo")
    void reiniciarConservaElModo() {
        Planta planta = new Planta(SEMILLA);
        planta.cambiarModo(ModoSimulacion.MANUAL);
        planta.tick();

        planta.reiniciar();

        assertEquals(ModoSimulacion.MANUAL, planta.modo());
    }

    @Test
    @DisplayName("cada fallo pide la especialidad de su sensor, y el preventivo es mecánico")
    void tipoDeFalloSaleDelSensor() {
        Planta planta = plantaSinFallosAleatorios();
        planta.cambiarModo(ModoSimulacion.MANUAL);
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.inyectarFallo(TORNO, CORRIENTE, Magnitud.SEVERA);

        planta.tick();

        List<EventoFallo> eventos = planta.eventosPendientes();
        assertEquals(TipoFallo.MECANICO, eventos.get(0).tipoFallo(), "la vibración es mecánica");
        assertEquals(TipoFallo.ELECTRICO, eventos.get(1).tipoFallo(), "la corriente es eléctrica");
        planta.clasificarSiguiente();
        assertEquals(TipoFallo.MECANICO, planta.ordenes().verSiguiente().orElseThrow().tipoFallo(),
                "la orden conserva el tipo del evento");
        assertEquals(TipoFallo.MECANICO,
                Clasificador.ordenPreventiva(planta.maquina(PRENSA), PRENSA, 99, 0).tipoFallo());
    }

    @Test
    @DisplayName("en automático la orden espera a un técnico de su especialidad, aunque haya otro libre")
    void automaticoEsperaASuEspecialidad() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(TORNO, CORRIENTE, Magnitud.SEVERA);
        planta.inyectarFallo(ROBOT, CORRIENTE, Magnitud.SEVERA);

        planta.tick();
        planta.tick();

        assertEquals(TORNO, ordenEnCurso(planta).orElseThrow().indiceMaquina(),
                "la primera orden eléctrica la toma el eléctrico");
        assertEquals(1, planta.ordenes().tamano(), "la segunda espera en la cola con prioridad");
        assertEquals("Ana", planta.tecnicosLibres(TipoFallo.MECANICO).get(0).nombre(),
                "aunque la mecánica está libre");

        for (int i = 0; i < 20 && !planta.ordenes().estaVacia(); i++) {
            planta.tick();
        }
        Tecnico luis = planta.tecnicos().get(1);
        assertEquals(ROBOT, luis.ordenActual().orElseThrow().indiceMaquina(),
                "al terminar, el eléctrico vuelve a su cola y toma la orden que esperaba");
    }

    @Test
    @DisplayName("en manual, un técnico de la otra especialidad tarda el doble")
    void penalidadPorEspecialidad() {
        Planta planta = plantaSinFallosAleatorios();
        planta.cambiarModo(ModoSimulacion.MANUAL);
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);
        planta.tick();
        planta.clasificarSiguiente();

        assertTrue(planta.asignarSiguiente(TipoFallo.ELECTRICO));

        Tecnico luis = planta.tecnicos().get(1);
        assertEquals(TipoFallo.ELECTRICO, luis.especialidad());
        assertEquals(ConfiguracionPlanta.PENALIDAD_ESPECIALIDAD
                        * ConfiguracionPlanta.duracionReparacion(Prioridad.P1),
                luis.ticksRestantes());
        assertTrue(planta.bitacora().getLast().contains("especialidad distinta"));
    }

    @Test
    @DisplayName("una máquina produce en proporción a su carga, y detenida no produce")
    void maquinaDetenidaNoProduce() {
        Planta planta = plantaSinFallosAleatorios();
        planta.maquina(PRENSA).cambiarCarga(100);

        planta.tick();
        assertEquals(ConfiguracionPlanta.INGRESO_POR_TICK, planta.ingresos(),
                "solo produce la prensa, a carga 100");

        planta.ejecutar(new PararOArrancarMaquina(planta.maquina(PRENSA)));
        planta.tick();
        assertEquals(ConfiguracionPlanta.INGRESO_POR_TICK, planta.ingresos(),
                "parada, la prensa ya no suma");
        assertEquals(ConfiguracionPlanta.PRESUPUESTO_INICIAL + ConfiguracionPlanta.INGRESO_POR_TICK,
                planta.caja());
    }

    @Test
    @DisplayName("cada tick de trabajo de un técnico se cobra como mano de obra")
    void manoDeObra() {
        Planta planta = plantaSinFallosAleatorios();
        planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.SEVERA);

        for (int i = 0; i < 3; i++) {
            planta.tick();
        }

        assertEquals(3 * ConfiguracionPlanta.MANO_DE_OBRA_POR_TICK, planta.costos(),
                "un técnico estuvo ocupado los 3 ticks");
        assertEquals(ConfiguracionPlanta.MANO_DE_OBRA_POR_TICK, planta.costoDelTick());
    }

    @Test
    @DisplayName("una compañía en quiebra no opera ni acepta acciones, pero su tick avanza")
    void quiebraCongelaLaCompania() {
        Planta planta = plantaSinFallosAleatorios();
        planta.contabilidad().cobrar(ConfiguracionPlanta.PRESUPUESTO_INICIAL + 1);
        int lecturas = planta.maquina(PRENSA).lecturas().tamano();

        planta.tick();

        assertTrue(planta.enQuiebra());
        assertEquals(1, planta.tickActual(), "el tick sigue al de la organización");
        assertEquals(lecturas, planta.maquina(PRENSA).lecturas().tamano(), "no lee sensores");
        assertThrows(IllegalStateException.class,
                () -> planta.inyectarFallo(PRENSA, VIBRACION, Magnitud.LEVE));
        assertThrows(IllegalStateException.class, planta::deshacer);

        planta.reiniciar();
        assertFalse(planta.enQuiebra(), "reiniciar empieza otra partida con el presupuesto inicial");
    }

    @Test
    @DisplayName("descarte perezoso: el preventivo de una máquina que recibió overhaul se descarta al salir")
    void preventivoDescartadoTrasOverhaul() {
        Planta planta = plantaSinFallosAleatorios();
        planta.cambiarModo(ModoSimulacion.MANUAL);
        while (planta.ordenes().estaVacia()) {
            planta.tick();
        }
        OrdenTrabajo preventivo = planta.ordenes().verSiguiente().orElseThrow();
        assertEquals(Prioridad.P4, preventivo.prioridad());
        Maquina maquina = planta.maquina(preventivo.indiceMaquina());

        planta.iniciarOverhaul(preventivo.indiceMaquina(), "Fabricante 1");
        planta.asignarSiguiente(TipoFallo.MECANICO);

        assertTrue(planta.bitacora().stream().anyMatch(linea -> linea.endsWith(
                preventivo.etiquetaCorta() + " descartada: " + maquina.etiqueta() + " ya recibió overhaul o fue reemplazada")));
        assertTrue(planta.ordenes().ordenesDe(Prioridad.P4).stream()
                .noneMatch(orden -> orden.id() == preventivo.id()), "salió por el frente");
        assertTrue(ordenEnCurso(planta).map(orden -> orden.id() != preventivo.id()).orElse(true),
                "y no se le asignó a nadie");
    }

    /**
     * Crea una planta donde ninguna máquina falla por azar, poniendo su carga en cero.
     *
     * @return planta lista para que la prueba provoque solo lo que le interesa
     */
    private static Planta plantaSinFallosAleatorios() {
        Planta planta = new Planta(SEMILLA);
        for (Maquina maquina : planta.inventario()) {
            maquina.cambiarCarga(0);
        }
        return planta;
    }

    /**
     * Busca la orden que algún técnico esté atendiendo.
     *
     * @param planta planta consultada
     * @return la primera orden en curso, si hay alguna
     */
    private static Optional<OrdenTrabajo> ordenEnCurso(Planta planta) {
        return planta.tecnicos().stream()
                .map(Tecnico::ordenActual)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
