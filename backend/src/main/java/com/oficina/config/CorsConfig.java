package com.oficina.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS só para desenvolvimento local. Em produção front e API usam o mesmo domínio. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origens;

    public CorsConfig(@Value("${app.cors.origens:}") String[] origens) {
        this.origens = origens;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origens.length == 0) return;
        registry.addMapping("/api/**").allowedOrigins(origens).allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
