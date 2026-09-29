package com.crypto_data_platform.controller;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoRankingEntry;
import com.crypto_data_platform.repository.CryptoRepository;
import com.crypto_data_platform.service.CryptoRankingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CryptoController.class)
@ActiveProfiles("test")
class CryptoControllerTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CryptoRepository repository;
    @MockBean
    private CryptoRankingService rankingService;

    private Locale originalDefaultLocale;

    @BeforeEach
    void captureDefaultLocale() {
        originalDefaultLocale = Locale.getDefault();
    }

    @AfterEach
    void restoreDefaultLocale() {
        // Locale.getDefault() is process-wide mutable state; restore it so other tests in the
        // suite (run in the same JVM) are never affected by what happens in this test class.
        Locale.setDefault(originalDefaultLocale);
    }

    private static CryptoPrice priceOf(String symbol, double price, double fluctuation) {
        CryptoPrice entity = new CryptoPrice();
        entity.setSymbol(symbol);
        entity.setPrice(BigDecimal.valueOf(price));
        entity.setMarketCap(BigDecimal.valueOf(price * 1000));
        entity.setVolume(BigDecimal.valueOf(price * 10));
        entity.setEventTime(LocalDateTime.of(2024, 1, 15, 10, 30));
        entity.setTimestamp(LocalDateTime.of(2024, 1, 15, 10, 31));
        entity.setPriceFluctuation(BigDecimal.valueOf(fluctuation));
        return entity;
    }

    @Test
    void getBySymbol_returnsHistoryNewestFirst_includingFluctuation() throws Exception {
        when(repository.findBySymbolOrderByEventTimeDesc("btc"))
                .thenReturn(List.of(priceOf("btc", 66000.0, 1.54), priceOf("btc", 65000.0, -0.5)));

        mockMvc.perform(get("/api/cryptos/btc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].symbol").value("btc"))
                .andExpect(jsonPath("$[0].price").value(66000.0))
                .andExpect(jsonPath("$[0].priceFluctuation").value(1.54));
    }

    @Test
    void getBySymbol_lowercasesTheSymbol_beforeQuerying() throws Exception {
        when(repository.findBySymbolOrderByEventTimeDesc(eq("btc"))).thenReturn(List.of());

        mockMvc.perform(get("/api/cryptos/BTC")).andExpect(status().isOk());
    }

    @Test
    void getBySymbol_returnsEmptyList_whenSymbolHasNoRecords() throws Exception {
        when(repository.findBySymbolOrderByEventTimeDesc("doge")).thenReturn(List.of());

        mockMvc.perform(get("/api/cryptos/doge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void getRanking_usesDefaultMinSamplesAndLimit_whenNoQueryParamsGiven() throws Exception {
        when(rankingService.getTopPerformers(CryptoRankingService.DEFAULT_MIN_SAMPLES, CryptoRankingService.DEFAULT_LIMIT))
                .thenReturn(List.of(new CryptoRankingEntry("btc", 10, 8, 80.0)));

        mockMvc.perform(get("/api/cryptos/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("btc"))
                .andExpect(jsonPath("$[0].gainDensityPercentage").value(80.0));
    }

    @Test
    void getRanking_forwardsCustomMinSamplesAndLimit_fromQueryParams() throws Exception {
        when(rankingService.getTopPerformers(5, 3)).thenReturn(List.of());

        mockMvc.perform(get("/api/cryptos/ranking?minSamples=5&limit=3"))
                .andExpect(status().isOk());
    }

    @Test
    void getRanking_returnsBadRequest_whenLimitIsNegative() throws Exception {
        // Regression test: CryptoController.getRanking now validates `limit` before calling
        // CryptoRankingService.getTopPerformers (which delegates to Stream.limit(long) and would
        // throw IllegalArgumentException for a negative value). A negative limit must be rejected
        // with a clean 400 Bad Request, and the service must never even be invoked.
        mockMvc.perform(get("/api/cryptos/ranking?limit=-1"))
                .andExpect(status().isBadRequest());

        verify(rankingService, org.mockito.Mockito.never()).getTopPerformers(anyInt(), anyInt());
    }

    @Test
    void getRanking_returnsBadRequest_whenMinSamplesIsNegative() throws Exception {
        // Regression test: CryptoController.getRanking now validates `minSamples` before calling
        // CryptoRankingService.getTopPerformers. A negative minSamples must be rejected with a
        // clean 400 Bad Request, and the service must never even be invoked.
        mockMvc.perform(get("/api/cryptos/ranking?minSamples=-5"))
                .andExpect(status().isBadRequest());

        verify(rankingService, org.mockito.Mockito.never()).getTopPerformers(anyInt(), anyInt());
    }

    @Test
    void getBySymbol_lowercasesCorrectly_regardlessOfDefaultLocale() throws Exception {
        // Regression test: CryptoController.getBySymbol uses Locale.ROOT to lowercase the symbol,
        // avoiding the "Turkish-I problem" where "PI".toLowerCase() under the Turkish locale
        // produces "pı" (dotless i) instead of "pi", which would silently miss data stored under
        // the correct lowercase symbol "pi" (e.g. the real PI / Pi Network ticker).
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        when(repository.findBySymbolOrderByEventTimeDesc(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/cryptos/PI")).andExpect(status().isOk());

        verify(repository).findBySymbolOrderByEventTimeDesc(eq("pi"));
        verify(repository, org.mockito.Mockito.never()).findBySymbolOrderByEventTimeDesc(eq("pı"));
    }
}
