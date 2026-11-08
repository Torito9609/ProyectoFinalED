package edu.unal.planta.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite que el frontend, que corre en otro puerto, llame a la API.
 *
 * <p>El navegador bloquea por seguridad las peticiones de una página a un origen distinto del suyo
 * (otro puerto ya es otro origen), salvo que el servidor diga que lo permite. Eso es CORS. Aquí se
 * permiten solo los orígenes locales configurados en {@code application.properties}
 * ({@code planta.cors.origenes}), porque el proyecto no se despliega en la nube.</p>
 *
 * @author Cristian Robayo
 * @author Kevin Toro
 */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    private final String[] origenes;

    /**
     * Crea la configuración. Spring le pasa los orígenes de {@code application.properties}.
     *
     * @param origenes patrones de los orígenes permitidos, por ejemplo {@code http://localhost:*}
     */
    public ConfiguracionCors(@Value("${planta.cors.origenes}") String[] origenes) {
        this.origenes = origenes.clone();
    }

    /**
     * Habilita CORS en las rutas {@code /api/**} para los métodos que usa la API.
     *
     * @param registro registro de CORS de Spring
     */
    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOriginPatterns(origenes)
                .allowedMethods("GET", "POST", "PUT");
    }
}
