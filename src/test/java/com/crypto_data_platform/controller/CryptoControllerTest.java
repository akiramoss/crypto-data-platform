package com.crypto_data_platform.controller;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.repository.CryptoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
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

    private static CryptoPrice priceOf(String symbol, double price, double fluctuation) {
        CryptoPrice entity = new CryptoPrice();
        entity.setSymbol(symbol);
        entity.setPrice(price);
        entity.setMarketCap(price * 1000);
        entity.setVolume(price * 10);
        entity.setEventTime(LocalDateTime.of(2024, 1, 15, 10, 30));
        entity.setTimestamp(LocalDateTime.of(2024, 1, 15, 10, 31));
        entity.setPriceFluctuation(fluctuation);
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
}
