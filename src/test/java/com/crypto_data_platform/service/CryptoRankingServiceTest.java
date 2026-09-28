package com.crypto_data_platform.service;

import com.crypto_data_platform.dto.CryptoRankingEntry;
import com.crypto_data_platform.repository.CryptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoRankingServiceTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private CryptoRepository repository;

    private CryptoRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new CryptoRankingService(repository);
    }

    private record Stats(String symbol, long totalCount, long positiveCount)
            implements CryptoRepository.SymbolFluctuationStats {
        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public Long getTotalCount() {
            return totalCount;
        }

        @Override
        public Long getPositiveCount() {
            return positiveCount;
        }
    }

    @Test
    void getTopPerformers_ranksBySymbol_byGainDensityDescending() {
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("btc", 10, 5),   // 50%
                new Stats("eth", 10, 9),   // 90%
                new Stats("sol", 10, 2)    // 20%
        ));

        List<CryptoRankingEntry> ranking = rankingService.getTopPerformers(1, 10);

        assertThat(ranking).extracting(CryptoRankingEntry::symbol)
                .containsExactly("eth", "btc", "sol");
        assertThat(ranking.get(0).gainDensityPercentage()).isEqualTo(90.0);
    }

    @Test
    void getTopPerformers_excludesSymbols_belowMinSamplesThreshold() {
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("btc", 10, 8),
                new Stats("doge", 2, 2) // only 2 samples, below the threshold of 3
        ));

        List<CryptoRankingEntry> ranking = rankingService.getTopPerformers(3, 10);

        assertThat(ranking).extracting(CryptoRankingEntry::symbol).containsExactly("btc");
    }

    @Test
    void getTopPerformers_limitsResultSize() {
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("a", 10, 9),
                new Stats("b", 10, 8),
                new Stats("c", 10, 7)
        ));

        List<CryptoRankingEntry> ranking = rankingService.getTopPerformers(1, 2);

        assertThat(ranking).hasSize(2);
        assertThat(ranking).extracting(CryptoRankingEntry::symbol).containsExactly("a", "b");
    }

    @Test
    void getTopPerformers_breaksDensityTies_byMoreTotalSamples() {
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("btc", 4, 2),  // 50%, 4 samples
                new Stats("eth", 10, 5)  // 50%, 10 samples
        ));

        List<CryptoRankingEntry> ranking = rankingService.getTopPerformers(1, 10);

        assertThat(ranking).extracting(CryptoRankingEntry::symbol).containsExactly("eth", "btc");
    }

    @Test
    void getTopPerformers_returnsEmptyList_whenNoSymbolHasAnyFluctuationData() {
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of());

        assertThat(rankingService.getTopPerformers(1, 10)).isEmpty();
    }
}
