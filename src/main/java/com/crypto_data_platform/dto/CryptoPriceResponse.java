package com.crypto_data_platform.dto;

import com.crypto_data_platform.domain.CryptoPrice;

import java.time.LocalDateTime;

/**
 * Representación de un {@link CryptoPrice} expuesta por la API REST propia, desacoplada de la
 * entidad JPA.
 */
public record CryptoPriceResponse(
        String symbol,
        Double price,
        Double marketCap,
        Double volume,
        LocalDateTime eventTime,
        LocalDateTime timestamp,
        Double priceFluctuation
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
