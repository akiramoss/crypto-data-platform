package com.crypto_data_platform.service;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoPriceResponse;
import com.crypto_data_platform.repository.CryptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoPriceQueryServiceTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 6, 15, 12, 0);

    @Mock
    private CryptoRepository repository;

    private CryptoPriceQueryService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new CryptoPriceQueryService(repository, fixedClock);
    }

    private static CryptoPrice priceOf(String symbol, LocalDateTime eventTime) {
        CryptoPrice entity = new CryptoPrice();
        entity.setSymbol(symbol);
        entity.setPrice(BigDecimal.valueOf(100.0));
        entity.setMarketCap(BigDecimal.valueOf(1000.0));
        entity.setVolume(BigDecimal.valueOf(10.0));
        entity.setEventTime(eventTime);
        entity.setTimestamp(eventTime);
        return entity;
    }

    @Test
    void getLatestPrices_mapsRepositoryResults_toResponseDtos() {
        when(repository.findLatestPerSymbol()).thenReturn(List.of(
                priceOf("btc", LocalDateTime.of(2024, 1, 15, 10, 30))));

        List<CryptoPriceResponse> result = service.getLatestPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).symbol()).isEqualTo("btc");
    }

    @Test
    void getHistory_lowercasesSymbol_beforeQueryingRepository() {
        when(repository.findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(eq("btc"), any(), any()))
                .thenReturn(List.of());

        service.getHistory("BTC", FIXED_NOW.minusDays(1), FIXED_NOW);

        verify(repository).findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(eq("btc"), any(), any());
    }

    @Test
    void getHistory_defaultsToLast30Days_whenFromAndToAreOmitted() {
        when(repository.findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(eq("btc"), any(), any()))
                .thenReturn(List.of());

        service.getHistory("btc", null, null);

        verify(repository).findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(
                "btc", FIXED_NOW.minusDays(CryptoPriceQueryService.DEFAULT_RANGE_DAYS), FIXED_NOW);
    }

    @Test
    void getHistory_defaultsFrom_whenOnlyToIsGiven() {
        LocalDateTime to = FIXED_NOW.minusDays(5);
        when(repository.findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(eq("btc"), any(), any()))
                .thenReturn(List.of());

        service.getHistory("btc", null, to);

        verify(repository).findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(
                "btc", to.minusDays(CryptoPriceQueryService.DEFAULT_RANGE_DAYS), to);
    }

    @Test
    void getHistory_throwsIllegalArgumentException_whenFromIsAfterTo() {
        assertThatThrownBy(() -> service.getHistory("btc", FIXED_NOW, FIXED_NOW.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'from' must not be after 'to'");
    }

    @Test
    void getHistory_throwsIllegalArgumentException_whenRangeExceedsMaxRangeDays() {
        LocalDateTime from = FIXED_NOW.minusDays(CryptoPriceQueryService.MAX_RANGE_DAYS + 1);

        assertThatThrownBy(() -> service.getHistory("btc", from, FIXED_NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed");
    }

    @Test
    void getHistory_allowsRange_exactlyAtMaxRangeDays() {
        LocalDateTime from = FIXED_NOW.minusDays(CryptoPriceQueryService.MAX_RANGE_DAYS);
        when(repository.findBySymbolAndEventTimeBetweenOrderByEventTimeDesc(eq("btc"), any(), any()))
                .thenReturn(List.of());

        service.getHistory("btc", from, FIXED_NOW);

        verify(repository).findBySymbolAndEventTimeBetweenOrderByEventTimeDesc("btc", from, FIXED_NOW);
    }
}
