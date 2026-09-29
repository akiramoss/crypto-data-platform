package com.crypto_data_platform.controller;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoPriceResponse;
import com.crypto_data_platform.dto.CryptoRankingEntry;
import com.crypto_data_platform.repository.CryptoRepository;
import com.crypto_data_platform.service.CryptoRankingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/cryptos")
public class CryptoController {

    private final CryptoRepository repository;
    private final CryptoRankingService rankingService;

    public CryptoController(CryptoRepository repository, CryptoRankingService rankingService) {
        this.repository = repository;
        this.rankingService = rankingService;
    }

    /**
     * Historial de precios (y su fluctuación) de un symbol concreto, del más reciente al más
     * antiguo. Devuelve una lista vacía si el symbol no tiene ningún registro almacenado todavía.
     */
    @GetMapping("/{symbol}")
    public List<CryptoPriceResponse> getBySymbol(@PathVariable String symbol) {
        // Los symbols se guardan tal cual los devuelve CoinGecko (en minúsculas, ej. "btc").
        // Locale.ROOT evita el "problema de la I turca" (bajo el locale turco, "PI".toLowerCase()
        // produce "pı", no "pi", lo que dejaría de encontrar datos guardados como "pi").
        List<CryptoPrice> entities = repository.findBySymbolOrderByEventTimeDesc(symbol.toLowerCase(Locale.ROOT));
        return entities.stream().map(CryptoPriceResponse::fromEntity).toList();
    }

    /**
     * Ranking de "densidad de ganancias": para cada symbol con al menos {@code minSamples}
     * variaciones registradas, el porcentaje de esas variaciones que fueron positivas, de mayor a
     * menor.
     */
    @GetMapping("/ranking")
    public List<CryptoRankingEntry> getRanking(
            @RequestParam(defaultValue = "" + CryptoRankingService.DEFAULT_MIN_SAMPLES) int minSamples,
            @RequestParam(defaultValue = "" + CryptoRankingService.DEFAULT_LIMIT) int limit) {
        // CryptoRankingService.getTopPerformers delega en Stream.limit(long), que lanza
        // IllegalArgumentException para valores negativos; se valida aquí, en el límite del
        // sistema, para devolver un 400 claro en vez de un 500 genérico.
        if (limit < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must not be negative");
        }
        // Un minSamples negativo no tiene sentido semántico y, sin validar, deja pasar a todos los
        // symbols (con >= 0 muestras), contradiciendo el propósito documentado de filtrar por
        // histórico mínimo.
        if (minSamples < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minSamples must not be negative");
        }
        return rankingService.getTopPerformers(minSamples, limit);
    }
}
