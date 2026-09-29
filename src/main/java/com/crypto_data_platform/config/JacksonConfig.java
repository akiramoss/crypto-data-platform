package com.crypto_data_platform.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Sin esto, LocalDateTime se serializa como array numérico [year, month, day, ...]
        // en vez de una cadena ISO-8601 legible.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Sin esto, un BigDecimal con muchos decimales (p.ej. el precio de una moneda de muy
        // bajo valor) podría serializarse en notación científica (1.23E-8) en vez de en plano.
        mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        // CoinGecko's real /coins/markets response has 25+ fields; CryptoApiResponse only maps
        // the ones we use. Sin esto, cualquier campo no mapeado (p.ej. "market_cap_rank") hace
        // fallar la deserialización y el pipeline de ingesta no guarda nada, silenciosamente.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
