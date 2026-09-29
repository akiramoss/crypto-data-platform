package com.crypto_data_platform.service;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.repository.CryptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceFluctuationServiceTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private CryptoRepository repository;

    private PriceFluctuationService fluctuationService;

    @BeforeEach
    void setUp() {
        fluctuationService = new PriceFluctuationService(repository);
    }

    private static CryptoPrice priceOf(String symbol, Double price) {
        CryptoPrice entity = new CryptoPrice();
        entity.setSymbol(symbol);
        entity.setPrice(price == null ? null : BigDecimal.valueOf(price));
        entity.setEventTime(LocalDateTime.of(2024, 1, 15, 10, 30));
        return entity;
    }

    @Test
    void applyFluctuations_setsNull_whenNoPreviousRecordExistsForSymbol() {
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC")).thenReturn(Optional.empty());
        CryptoPrice entity = priceOf("BTC", 65000.0);

        fluctuationService.applyFluctuations(List.of(entity));

        assertThat(entity.getPriceFluctuation()).isNull();
    }

    @Test
    void applyFluctuations_computesPercentageChange_relativeToPreviousStoredRecord() {
        CryptoPrice previous = priceOf("BTC", 50000.0);
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC")).thenReturn(Optional.of(previous));
        CryptoPrice entity = priceOf("BTC", 55000.0);

        fluctuationService.applyFluctuations(List.of(entity));

        // (55000 - 50000) / 50000 * 100 = 10%
        assertThat(entity.getPriceFluctuation()).isEqualByComparingTo("10");
    }

    @Test
    void applyFluctuations_computesNegativePercentage_whenPriceDropped() {
        CryptoPrice previous = priceOf("ETH", 4000.0);
        when(repository.findTopBySymbolOrderByEventTimeDesc("ETH")).thenReturn(Optional.of(previous));
        CryptoPrice entity = priceOf("ETH", 3600.0);

        fluctuationService.applyFluctuations(List.of(entity));

        // (3600 - 4000) / 4000 * 100 = -10%
        assertThat(entity.getPriceFluctuation()).isEqualByComparingTo("-10");
    }

    @Test
    void applyFluctuations_chainsWithinSameBatch_insteadOfReusingStaleDbValue() {
        // Two records for the same symbol in one ingestion cycle: the second must compare against
        // the first (in-batch), not against whatever was already in the database.
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC")).thenReturn(Optional.of(priceOf("BTC", 100.0)));
        CryptoPrice first = priceOf("BTC", 110.0);
        CryptoPrice second = priceOf("BTC", 121.0);

        fluctuationService.applyFluctuations(List.of(first, second));

        assertThat(first.getPriceFluctuation()).isEqualByComparingTo("10");
        assertThat(second.getPriceFluctuation()).isEqualByComparingTo("10");
    }

    @Test
    void applyFluctuations_setsNull_whenCurrentPriceIsNull() {
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC"))
                .thenReturn(Optional.of(priceOf("BTC", 100.0)));
        CryptoPrice entity = priceOf("BTC", null);

        fluctuationService.applyFluctuations(List.of(entity));

        assertThat(entity.getPriceFluctuation()).isNull();
    }

    @Test
    void applyFluctuations_setsNull_whenPreviousPriceIsZero_toAvoidDivisionByZero() {
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC"))
                .thenReturn(Optional.of(priceOf("BTC", 0.0)));
        CryptoPrice entity = priceOf("BTC", 100.0);

        fluctuationService.applyFluctuations(List.of(entity));

        assertThat(entity.getPriceFluctuation()).isNull();
    }

    @Test
    void applyFluctuations_setsZero_whenPriceIsUnchanged() {
        // Edge case around the previousPrice == 0 short-circuit in calculateFluctuation(): a
        // genuine (non-zero) unchanged price must yield 0.0, not null and not be mistaken for the
        // "no previous price" case.
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC"))
                .thenReturn(Optional.of(priceOf("BTC", 100.0)));
        CryptoPrice entity = priceOf("BTC", 100.0);

        fluctuationService.applyFluctuations(List.of(entity));

        assertThat(entity.getPriceFluctuation()).isNotNull().isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void applyFluctuations_fallsBackToDbLookup_whenEarlierRecordInSameBatchHadNullPrice() {
        // If the first record of a symbol within the batch carries a null price, it must not
        // "poison" the in-batch cache with a null: the next record for that same symbol should
        // still fall back to the last known DB price, not be treated as "no previous price".
        when(repository.findTopBySymbolOrderByEventTimeDesc("BTC"))
                .thenReturn(Optional.of(priceOf("BTC", 100.0)));
        CryptoPrice firstWithNullPrice = priceOf("BTC", null);
        CryptoPrice second = priceOf("BTC", 110.0);

        fluctuationService.applyFluctuations(List.of(firstWithNullPrice, second));

        assertThat(firstWithNullPrice.getPriceFluctuation()).isNull();
        assertThat(second.getPriceFluctuation()).isEqualByComparingTo("10");
    }

    @Test
    void applyFluctuations_looksUpEachSymbolIndependently() {
        when(repository.findTopBySymbolOrderByEventTimeDesc(eq("BTC")))
                .thenReturn(Optional.of(priceOf("BTC", 100.0)));
        when(repository.findTopBySymbolOrderByEventTimeDesc(eq("ETH")))
                .thenReturn(Optional.of(priceOf("ETH", 200.0)));
        CryptoPrice btc = priceOf("BTC", 110.0);
        CryptoPrice eth = priceOf("ETH", 190.0);

        fluctuationService.applyFluctuations(List.of(btc, eth));

        assertThat(btc.getPriceFluctuation()).isEqualByComparingTo("10");
        assertThat(eth.getPriceFluctuation()).isEqualByComparingTo("-5");
    }
}
