package com.crypto_data_platform.service;

import com.crypto_data_platform.dto.CryptoPriceResponse;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Consultas de precios pensadas para el dashboard: última cotización por symbol, e histórico
 * acotado por rango de fechas (a diferencia de CryptoController#getBySymbol, que devuelve el
 * histórico completo sin acotar).
 */
@Service
public class CryptoPriceQueryService {

    public static final long DEFAULT_RANGE_DAYS = 30;
    public static final long MAX_RANGE_DAYS = 180;

    private final CryptoRepository repository;
    private final Clock clock;

    public CryptoPriceQueryService(CryptoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<CryptoPriceResponse> getLatestPrices() {
        return repository.findLatestPerSymbol().stream()
                .map(CryptoPriceResponse::fromEntity)
                .toList();
    }

    /**
     * @param from límite inferior del rango (inclusive); si es null, se calcula como
     *             {@code to - DEFAULT_RANGE_DAYS} días.
     * @param to   límite superior del rango (inclusive); si es null, se usa la hora actual.
     */
    public List<CryptoPriceResponse> getHistory(String symbol, LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now(clock);
        LocalDateTime effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_RANGE_DAYS);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        if (Duration.between(effectiveFrom, effectiveTo).toDays() > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("date range must not exceed " + MAX_RANGE_DAYS + " days");
        }

        // Locale.ROOT evita el "problema de la I turca" (ver CryptoController#getBySymbol).
        String normalizedSymbol = symbol.toLowerCase(Locale.ROOT);
        return repository.findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(
                        normalizedSymbol, effectiveFrom, effectiveTo)
                .stream()
                .map(CryptoPriceResponse::fromEntity)
                .toList();
    }
}
