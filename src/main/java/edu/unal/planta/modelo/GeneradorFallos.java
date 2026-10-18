package edu.unal.planta.modelo;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Decide si una máquina desarrolla una anomalía y de qué gravedad.
 *
 * <p>Es la única clase del modelo con aleatoriedad, y usa un solo {@link Random} con semilla
 * configurable. De ahí sale una propiedad importante para la demostración y el video: <strong>la
 * misma semilla produce exactamente la misma simulación</strong>, así que se puede ensayar una
 * secuencia y repetirla igual frente al profesor.</p>
 *
 * <p>Aquí solo está el sorteo. La probabilidad la calcula la máquina en
 * {@link Maquina#probabilidadFallo()}, porque depende de su carga y de su mantenimiento atrasado,
 * no del azar.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
public final class GeneradorFallos {

    private final Random azar;

    /**
     * Crea el generador con una semilla.
     *
     * @param semilla semilla del generador aleatorio; la misma semilla repite la simulación
     */
    public GeneradorFallos(long semilla) {
        this.azar = new Random(semilla);
    }

    /**
     * Sortea si la máquina desarrolla una anomalía en este tick.
     *
     * <p>Primero compara un número al azar contra {@link Maquina#probabilidadFallo()}. Si hay
     * fallo, elige un sensor al azar y una magnitud según los pesos de
     * {@link ConfiguracionPlanta}.</p>
     *
     * @param maquina máquina que podría fallar
     * @return la anomalía recién aparecida, o un {@link Optional} vacío si no falló
     * @throws NullPointerException si la máquina es nula
     */
    public Optional<Anomalia> intentarGenerar(Maquina maquina) {
        Objects.requireNonNull(maquina, "La máquina no puede ser nula");
        if (azar.nextDouble() >= maquina.probabilidadFallo()) {
            return Optional.empty();
        }
        int indiceSensor = azar.nextInt(maquina.sensores().size());
        return Optional.of(crear(maquina, indiceSensor, sortearMagnitud()));
    }

    /**
     * Construye una anomalía concreta, sin ninguna aleatoriedad.
     *
     * <p>La usa el generador tras el sorteo, y también la acción "Inyectar fallo" del operador,
     * donde el sensor y la magnitud los elige una persona para forzar un caso en la
     * demostración.</p>
     *
     * @param maquina      máquina afectada
     * @param indiceSensor sensor que se va a desviar
     * @param magnitud     gravedad del fallo
     * @return la anomalía, con el desplazamiento ya calculado
     * @throws NullPointerException      si la máquina o la magnitud son nulas
     * @throws IndexOutOfBoundsException si el índice no corresponde a ningún sensor
     */
    public Anomalia crear(Maquina maquina, int indiceSensor, Magnitud magnitud) {
        Objects.requireNonNull(maquina, "La máquina no puede ser nula");
        Objects.requireNonNull(magnitud, "La magnitud no puede ser nula");
        Sensor sensor = maquina.sensor(indiceSensor);
        return new Anomalia(indiceSensor, magnitud, desplazamientoPara(sensor, magnitud));
    }

    /**
     * Elige una magnitud según los pesos configurados: 80 % leve, 15 % moderada, 5 % severa.
     *
     * <p>Es un sorteo por ruleta: se toma un número entre cero y la suma de los pesos, y se van
     * descontando los pesos hasta encontrar la magnitud que le corresponde.</p>
     *
     * @return la magnitud sorteada
     */
    private Magnitud sortearMagnitud() {
        int boleta = azar.nextInt(ConfiguracionPlanta.pesoTotalMagnitudes());
        for (Magnitud magnitud : Magnitud.values()) {
            boleta -= ConfiguracionPlanta.pesoMagnitud(magnitud);
            if (boleta < 0) {
                return magnitud;
            }
        }
        // Inalcanzable: la boleta siempre es menor que la suma de los pesos.
        return Magnitud.LEVE;
    }

    /**
     * Calcula cuánto hay que desviar la lectura para que caiga en la zona de umbrales que
     * corresponde a esa magnitud.
     *
     * <p>Se apunta al <strong>centro</strong> de la zona, de modo que el ruido normal del sensor no
     * alcance a sacar la lectura de ella:</p>
     * <ul>
     *   <li>leve: entre el umbral de advertencia y el alto;</li>
     *   <li>moderada: entre el alto y el crítico;</li>
     *   <li>severa: por encima del crítico.</li>
     * </ul>
     *
     * <p>Esto es lo que hace que la magnitud sorteada termine convirtiéndose en una prioridad, pero
     * <strong>sin que el clasificador se entere</strong>: él solo ve el número que llegó del
     * sensor.</p>
     *
     * @param sensor   sensor afectado
     * @param magnitud gravedad del fallo
     * @return cuánto se le suma al valor nominal en cada lectura
     */
    private static double desplazamientoPara(Sensor sensor, Magnitud magnitud) {
        double objetivo = switch (magnitud) {
            case LEVE -> (sensor.umbralAdvertencia() + sensor.umbralAlto()) / 2;
            case MODERADA -> (sensor.umbralAlto() + sensor.umbralCritico()) / 2;
            case SEVERA -> sensor.umbralCritico()
                    + (sensor.umbralCritico() - sensor.umbralAlto()) * 0.3;
        };
        return objetivo - sensor.nominal();
    }

    /**
     * Devuelve un valor de ruido para una lectura, entre menos el ruido y más el ruido del sensor.
     *
     * <p>Vive aquí porque es la otra fuente de azar de la simulación, y conviene que todo el azar
     * salga del mismo generador con semilla.</p>
     *
     * @param sensor sensor que se está leyendo
     * @return desviación aleatoria a sumar al valor nominal
     */
    public double ruidoPara(Sensor sensor) {
        Objects.requireNonNull(sensor, "El sensor no puede ser nulo");
        return (azar.nextDouble() * 2 - 1) * sensor.ruido();
    }
}
