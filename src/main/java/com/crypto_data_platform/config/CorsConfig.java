package com.crypto_data_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS solo para el origen del dashboard en dev (configurable vía
 * dashboard.cors.allowed-origin), y solo para las rutas /api/**.
 */
@Configuration
public class CorsConfig {

    private final DashboardCorsProperties corsProperties;

    public CorsConfig(DashboardCorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(corsProperties.getAllowedOrigin())
                        .allowedMethods("GET");
            }
        };
    }
}
