package com.crypto_data_platform.service;

import com.crypto_data_platform.dto.CryptoRankingEntry;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Analiza históricamente las variaciones de precio (priceFluctuation) de todos los symbols
 * conocidos y calcula, para cada uno, su "densidad de ganancias": el porcentaje de sus
 * variaciones registradas que fueron positivas. Cuanto más alta, más consistentemente sube ese
 * symbol de una sesión a otra.
 */
@Service
public class CryptoRankingService {

    public static final int DEFAULT_MIN_SAMPLES = 3;
    public static final int DEFAULT_LIMIT = 10;

    private final CryptoRepository repository;

    public CryptoRankingService(CryptoRepository repository) {
        this.repository = repository;
    }

    /**
     * @param minSamples número mínimo de variaciones registradas que debe tener un symbol para
     *                   entrar en el ranking (evita que un symbol con 1 solo dato domine la lista)
     * @param limit      número máximo de symbols a devolver
     */
    public List<CryptoRankingEntry> getTopPerformers(int minSamples, int limit) {
        Comparator<CryptoRankingEntry> byDensityThenSampleSize = Comparator
                .comparingDouble(CryptoRankingEntry::gainDensityPercentage).reversed()
                .thenComparing(Comparator.comparingLong(CryptoRankingEntry::totalFluctuations).reversed());

        return repository.aggregateFluctuationStatsBySymbol().stream()
                .filter(stats -> stats.getTotalCount() >= minSamples)
                .map(stats -> new CryptoRankingEntry(
                        stats.getSymbol(),
                        stats.getTotalCount(),
                        stats.getPositiveCount(),
                        (stats.getPositiveCount() * 100.0) / stats.getTotalCount()))
                .sorted(byDensityThenSampleSize)
                .limit(limit)
                .toList();
    }
}
