package com.crypto_data_platform.dto;

import com.crypto_data_platform.domain.CryptoPrice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representación de un {@link CryptoPrice} expuesta por la API REST propia, desacoplada de la
 * entidad JPA.
 */
public record CryptoPriceResponse(
        String symbol,
        BigDecimal price,
        BigDecimal marketCap,
        BigDecimal volume,
        LocalDateTime eventTime,
        LocalDateTime timestamp,
        BigDecimal priceFluctuation
) {

    public static CryptoPriceResponse fromEntity(CryptoPrice entity) {
        return new CryptoPriceResponse(
                entity.getSymbol(),
                entity.getPrice(),
                entity.getMarketCap(),
                entity.getVolume(),
                entity.getEventTime(),
                entity.getTimestamp(),
                entity.getPriceFluctuation()
        );
    }
}
