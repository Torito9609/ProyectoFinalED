package edu.unal.planta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend Spring Boot.
 *
 * <p>Su única responsabilidad es arrancar Spring. Spring busca en este paquete y en sus
 * subpaquetes las clases que debe crear él mismo: el servicio de simulación y los controladores
 * REST. Esta clase no contiene lógica de simulación.</p>
 *
 * <p>Los paquetes {@code estructuras} y {@code modelo} no usan anotaciones de Spring y no
 * dependen del framework (sección 4 de {@code CLAUDE.md}). Spring solo los toca a través del
 * servicio.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@SpringBootApplication
public class AplicacionPlanta {

    /**
     * Constructor público, porque Spring instancia esta clase como clase de configuración.
     * No se debe llamar a mano.
     */
    public AplicacionPlanta() {
    }

    /**
     * Arranca el servidor web embebido con la API del simulador.
     *
     * @param args argumentos de línea de comandos; Spring acepta propiedades con la forma
     *             {@code --nombre=valor}
     */
    public static void main(String[] args) {
        SpringApplication.run(AplicacionPlanta.class, args);
    }
}
