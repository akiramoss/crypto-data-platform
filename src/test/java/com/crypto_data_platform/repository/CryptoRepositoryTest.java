package com.crypto_data_platform.repository;

import com.crypto_data_platform.domain.CryptoPrice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @DataJpaTest slice test, backed by the H2 in-memory database configured for the "test"
 * profile (see application-test.properties). No real MySQL instance is required.
 */
@DataJpaTest
@ActiveProfiles("test")
class CryptoRepositoryTest {

    // NOTE: this JVM runs Java 25, newer than what the Byte Buddy version bundled with the
    // current Hibernate release officially supports (Java 23) for its bytecode enhancement.
    // Byte Buddy resolves/caches this compatibility flag once per JVM the first time it is
    // used, so this must be set before Hibernate/Spring context bootstrapping for this test.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Autowired
    private CryptoRepository repository;

    private static CryptoPrice priceOf(String symbol, LocalDateTime eventTime) {
        CryptoPrice entity = new CryptoPrice();
        entity.setSymbol(symbol);
        entity.setPrice(BigDecimal.valueOf(100.0));
        entity.setMarketCap(BigDecimal.valueOf(1000.0));
        entity.setVolume(BigDecimal.valueOf(10.0));
        entity.setEventTime(eventTime);
        entity.setTimestamp(LocalDateTime.now());
        return entity;
    }

    @Test
    void save_persistsCryptoPrice_andAssignsGeneratedId() {
        // Arrange
        CryptoPrice entity = priceOf("BTC", LocalDateTime.of(2024, 1, 15, 10, 30));

        // Act
        CryptoPrice saved = repository.save(entity);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void save_allowsSameSymbol_atDifferentEventTimes() {
        // Arrange
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 15, 10, 30)));

        // Act: different event_time for the same symbol does not violate the constraint.
        CryptoPrice second = repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 15, 10, 31)));

        // Assert
        assertThat(second.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void save_violatesUniqueConstraint_whenSameSymbolAndEventTimeAlreadyExists() {
        // Arrange
        LocalDateTime sameEventTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        repository.saveAndFlush(priceOf("BTC", sameEventTime));

        // Act + Assert: the @UniqueConstraint(columnNames = {"symbol", "event_time"}) on
        // CryptoPrice (domain/CryptoPrice.java:8-9) is enforced at the DB level.
        assertThatThrownBy(() -> repository.saveAndFlush(priceOf("BTC", sameEventTime)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findTopBySymbolOrderByEventTimeDesc_returnsMostRecentRecord_forThatSymbol() {
        // Arrange
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 15, 10, 30)));
        CryptoPrice latest = repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 16, 8, 0)));
        repository.saveAndFlush(priceOf("ETH", LocalDateTime.of(2024, 1, 20, 0, 0)));

        // Act
        Optional<CryptoPrice> result = repository.findTopBySymbolOrderByEventTimeDesc("BTC");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
    }

    @Test
    void findTopBySymbolOrderByEventTimeDesc_returnsEmpty_whenSymbolHasNoRecords() {
        assertThat(repository.findTopBySymbolOrderByEventTimeDesc("DOGE")).isEmpty();
    }

    @Test
    void findBySymbolOrderByEventTimeDesc_returnsAllRecordsForSymbol_newestFirst() {
        // Arrange
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 15, 10, 30)));
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 16, 8, 0)));
        repository.saveAndFlush(priceOf("ETH", LocalDateTime.of(2024, 1, 20, 0, 0)));

        // Act
        List<CryptoPrice> result = repository.findBySymbolOrderByEventTimeDesc("BTC");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventTime()).isEqualTo(LocalDateTime.of(2024, 1, 16, 8, 0));
        assertThat(result.get(1).getEventTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
    }

    @Test
    void findBySymbolAndEventTimeBetweenOrderByEventTimeDesc_returnsOnlyRecordsInRange_newestFirst() {
        // Arrange
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 10, 0, 0)));
        CryptoPrice inRangeLater = repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 20, 0, 0)));
        CryptoPrice inRangeEarlier = repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 15, 0, 0)));
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 2, 1, 0, 0)));
        repository.saveAndFlush(priceOf("ETH", LocalDateTime.of(2024, 1, 15, 0, 0)));

        // Act
        List<CryptoPrice> result = repository.findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(
                "BTC", LocalDateTime.of(2024, 1, 12, 0, 0), LocalDateTime.of(2024, 1, 25, 0, 0));

        // Assert
        assertThat(result).extracting(CryptoPrice::getId)
                .containsExactly(inRangeLater.getId(), inRangeEarlier.getId());
    }

    @Test
    void findLatestPerSymbol_returnsOneRecordPerSymbol_itsMostRecent() {
        // Arrange
        repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 15, 10, 30)));
        CryptoPrice latestBtc = repository.saveAndFlush(priceOf("BTC", LocalDateTime.of(2024, 1, 16, 8, 0)));
        CryptoPrice onlyEth = repository.saveAndFlush(priceOf("ETH", LocalDateTime.of(2024, 1, 10, 0, 0)));

        // Act
        List<CryptoPrice> result = repository.findLatestPerSymbol();

        // Assert
        assertThat(result).extracting(CryptoPrice::getId)
                .containsExactlyInAnyOrder(latestBtc.getId(), onlyEth.getId());
    }

    @Test
    void findLatestPerSymbol_returnsEmpty_whenNoRecordsExist() {
        assertThat(repository.findLatestPerSymbol()).isEmpty();
    }

    @Test
    void aggregateFluctuationStatsBySymbol_countsTotalAndPositiveFluctuations_perSymbol() {
        // Arrange: BTC has 3 fluctuation records (2 positive, 1 negative); ETH has 1 (positive);
        // SOL has a record with a null fluctuation, which must be excluded entirely.
        repository.saveAndFlush(fluctuatingPriceOf("BTC", LocalDateTime.of(2024, 1, 1, 0, 0), 5.0));
        repository.saveAndFlush(fluctuatingPriceOf("BTC", LocalDateTime.of(2024, 1, 2, 0, 0), -2.0));
        repository.saveAndFlush(fluctuatingPriceOf("BTC", LocalDateTime.of(2024, 1, 3, 0, 0), 1.5));
        repository.saveAndFlush(fluctuatingPriceOf("ETH", LocalDateTime.of(2024, 1, 1, 0, 0), 3.0));
        repository.saveAndFlush(fluctuatingPriceOf("SOL", LocalDateTime.of(2024, 1, 1, 0, 0), null));

        // Act
        List<CryptoRepository.SymbolFluctuationStats> stats = repository.aggregateFluctuationStatsBySymbol();

        // Assert
        assertThat(stats).extracting(CryptoRepository.SymbolFluctuationStats::getSymbol)
                .containsExactlyInAnyOrder("BTC", "ETH");

        CryptoRepository.SymbolFluctuationStats btcStats = stats.stream()
                .filter(s -> s.getSymbol().equals("BTC")).findFirst().orElseThrow();
        assertThat(btcStats.getTotalCount()).isEqualTo(3L);
        assertThat(btcStats.getPositiveCount()).isEqualTo(2L);

        CryptoRepository.SymbolFluctuationStats ethStats = stats.stream()
                .filter(s -> s.getSymbol().equals("ETH")).findFirst().orElseThrow();
        assertThat(ethStats.getTotalCount()).isEqualTo(1L);
        assertThat(ethStats.getPositiveCount()).isEqualTo(1L);
    }

    @Test
    void aggregateFluctuationStatsBySymbol_treatsExactlyZeroFluctuation_asNotPositive() {
        // Boundary check on the JPQL "c.priceFluctuation > 0" predicate (CryptoRepository.java:20):
        // an unchanged price (fluctuation == 0.0) must count towards totalCount but NOT towards
        // positiveCount. A ">=" instead of ">" here would silently inflate every symbol's gain
        // density with "no change" sessions.
        repository.saveAndFlush(fluctuatingPriceOf("BTC", LocalDateTime.of(2024, 1, 1, 0, 0), 0.0));
        repository.saveAndFlush(fluctuatingPriceOf("BTC", LocalDateTime.of(2024, 1, 2, 0, 0), 5.0));

        List<CryptoRepository.SymbolFluctuationStats> stats = repository.aggregateFluctuationStatsBySymbol();

        CryptoRepository.SymbolFluctuationStats btcStats = stats.stream()
                .filter(s -> s.getSymbol().equals("BTC")).findFirst().orElseThrow();
        assertThat(btcStats.getTotalCount()).isEqualTo(2L);
        assertThat(btcStats.getPositiveCount()).isEqualTo(1L);
    }

    @Test
    void aggregateFluctuationStatsBySymbol_treatsDifferentCasing_asDistinctSymbols_onH2() {
        // ENVIRONMENT GAP (not a production bug per se, but a test-vs-prod divergence risk):
        // under H2 (this test's application-test.properties uses MODE=MySQL, but MODE only changes
        // SQL *syntax* compatibility, not collation), VARCHAR comparison/GROUP BY is case-sensitive,
        // so "BTC" and "btc" are aggregated as two separate symbols here. Real MySQL's default
        // collation (case-insensitive, e.g. utf8mb4_0900_ai_ci) would instead fold them into a
        // single GROUP BY bucket. In practice this is currently masked because CoinGecko symbols
        // and CryptoMapper always produce lowercase symbols, but it means this @DataJpaTest/H2 slice
        // cannot catch a casing-related aggregation bug that could appear only against real MySQL.
        repository.saveAndFlush(fluctuatingPriceOf("BTC", LocalDateTime.of(2024, 1, 1, 0, 0), 5.0));
        repository.saveAndFlush(fluctuatingPriceOf("btc", LocalDateTime.of(2024, 1, 2, 0, 0), 3.0));

        List<CryptoRepository.SymbolFluctuationStats> stats = repository.aggregateFluctuationStatsBySymbol();

        assertThat(stats).extracting(CryptoRepository.SymbolFluctuationStats::getSymbol)
                .containsExactlyInAnyOrder("BTC", "btc");
    }

    @Test
    void save_allowsSameEventTime_forSymbolsDifferingOnlyByCasing_onH2() {
        // Same underlying environment gap as above, applied to the unique constraint
        // (symbol, event_time) declared in CryptoPrice.java:18: H2's case-sensitive comparison lets
        // "BTC" and "btc" coexist at the same event_time. Under real MySQL's case-insensitive
        // default collation, this second insert would instead collide with the first and throw a
        // DataIntegrityViolationException. Not asserted as a failure here (H2 genuinely allows it);
        // documented so this divergence is not mistaken for full unique-constraint coverage.
        LocalDateTime sameEventTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        repository.saveAndFlush(priceOf("BTC", sameEventTime));

        CryptoPrice second = repository.saveAndFlush(priceOf("btc", sameEventTime));

        assertThat(second.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(2);
    }

    private static CryptoPrice fluctuatingPriceOf(String symbol, LocalDateTime eventTime, Double fluctuation) {
        CryptoPrice entity = priceOf(symbol, eventTime);
        entity.setPriceFluctuation(fluctuation == null ? null : BigDecimal.valueOf(fluctuation));
        return entity;
    }
}
