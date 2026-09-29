package com.crypto_data_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expone el reloj del sistema como bean para que los servicios que dependen de "ahora"
 * (p.ej. CryptoPriceQueryService) puedan sustituirlo por un Clock fijo en tests.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
