package com.crypto_data_platform.controller;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoPriceResponse;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cryptos")
public class CryptoController {

    private final CryptoRepository repository;

    public CryptoController(CryptoRepository repository) {
        this.repository = repository;
    }

    /**
     * Historial de precios (y su fluctuación) de un symbol concreto, del más reciente al más
     * antiguo. Devuelve una lista vacía si el symbol no tiene ningún registro almacenado todavía.
     */
    @GetMapping("/{symbol}")
    public List<CryptoPriceResponse> getBySymbol(@PathVariable String symbol) {
        // Los symbols se guardan tal cual los devuelve CoinGecko (en minúsculas, ej. "btc").
        List<CryptoPrice> entities = repository.findBySymbolOrderByEventTimeDesc(symbol.toLowerCase());
        return entities.stream().map(CryptoPriceResponse::fromEntity).toList();
    }
}
