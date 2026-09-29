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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void getTopPerformers_returnsEmptyList_whenLimitIsZero() {
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("btc", 10, 8)
        ));

        assertThat(rankingService.getTopPerformers(1, 0)).isEmpty();
    }

    @Test
    void getTopPerformers_throwsIllegalArgumentException_whenLimitIsNegative() {
        // CryptoRankingService.getTopPerformers (service/CryptoRankingService.java:46) forwards
        // `limit` straight into Stream.limit(long), which throws IllegalArgumentException for any
        // negative value. The service itself does NOT validate `limit` - by design, that guard now
        // lives at the REST boundary (CryptoController#getRanking rejects a negative limit with a
        // 400 before ever calling this method; see CryptoControllerTest#getRanking_returnsBadRequest_whenLimitIsNegative).
        // This test documents that the service still trusts its caller, per the project's
        // "validate only at system boundaries" convention.
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("btc", 10, 8)
        ));

        assertThatThrownBy(() -> rankingService.getTopPerformers(1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTopPerformers_includesAllSymbols_whenMinSamplesIsZeroOrNegative() {
        // minSamples <= 0 is accepted without validation at the service level: every symbol with
        // at least one recorded fluctuation passes the `totalCount >= minSamples` filter. A
        // negative minSamples is now rejected earlier, at the REST boundary (CryptoController
        // #getRanking returns 400 before ever calling this method; see
        // CryptoControllerTest#getRanking_returnsBadRequest_whenMinSamplesIsNegative). This test
        // documents that the service itself still trusts its caller, per the project's "validate
        // only at system boundaries" convention.
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("btc", 1, 1)
        ));

        assertThat(rankingService.getTopPerformers(-5, 10))
                .extracting(CryptoRankingEntry::symbol)
                .containsExactly("btc");
    }

    @Test
    void getTopPerformers_ranksNaNDensityFirst_whenTotalCountIsZero_dueToUnguardedDivision() {
        // BUG (defensive-programming gap): CryptoRankingService.getTopPerformers (service/
        // CryptoRankingService.java:44) computes `(positiveCount * 100.0) / totalCount` with no
        // zero-guard. The current aggregateFluctuationStatsBySymbol() JPQL query (repository/
        // CryptoRepository.java:19-21) can never itself produce a totalCount of 0 (its WHERE clause
        // only counts non-null fluctuations), so this isn't reachable today - but it's a latent trap
        // for any future change to that query or to CryptoRepository.SymbolFluctuationStats. A
        // totalCount of 0 yields gainDensityPercentage = NaN, and because Double.compare/
        // comparingDouble treats NaN as greater than any other double, the reversed()
        // "highest density first" comparator ranks the NaN entry ABOVE every real gainer instead of
        // rejecting/excluding it.
        when(repository.aggregateFluctuationStatsBySymbol()).thenReturn(List.of(
                new Stats("real-gainer", 10, 9),
                new Stats("broken-zero-samples", 0, 0)
        ));

        List<CryptoRankingEntry> ranking = rankingService.getTopPerformers(0, 10);

        assertThat(ranking.get(0).symbol()).isEqualTo("broken-zero-samples");
        assertThat(ranking.get(0).gainDensityPercentage()).isNaN();
    }
}
