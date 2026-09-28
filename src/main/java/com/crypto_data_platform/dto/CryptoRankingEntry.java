package com.crypto_data_platform.dto;

/**
 * Una entrada del ranking de "densidad de ganancias": para un symbol, qué porcentaje de sus
 * variaciones registradas (priceFluctuation) fueron positivas.
 */
public record CryptoRankingEntry(
        String symbol,
        long totalFluctuations,
        long positiveFluctuations,
        double gainDensityPercentage
) {
}
