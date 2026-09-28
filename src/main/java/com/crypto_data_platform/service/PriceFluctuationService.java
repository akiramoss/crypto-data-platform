package com.crypto_data_platform.service;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula, para cada {@link CryptoPrice} recién obtenido, la variación porcentual de su precio
 * respecto al registro anterior almacenado para el mismo symbol (sesión a sesión, sin importar
 * cuánto tiempo haya pasado entre una y otra).
 */
@Service
public class PriceFluctuationService {

    private final CryptoRepository repository;

    public PriceFluctuationService(CryptoRepository repository) {
        this.repository = repository;
    }

    /**
     * Rellena {@code priceFluctuation} en cada entidad de la lista, en el orden dado. Si varias
     * entidades del mismo symbol aparecen en la misma lista (mismo ciclo de ingesta), cada una se
     * compara con la anterior dentro de esa misma lista, no solo con lo que ya hubiera en BD.
     */
    public void applyFluctuations(List<CryptoPrice> entities) {
        Map<String, Double> lastKnownPriceBySymbol = new HashMap<>();

        for (CryptoPrice entity : entities) {
            String symbol = entity.getSymbol();
            Double previousPrice = lastKnownPriceBySymbol.containsKey(symbol)
                    ? lastKnownPriceBySymbol.get(symbol)
                    : repository.findTopBySymbolOrderByEventTimeDesc(symbol)
                            .map(CryptoPrice::getPrice)
                            .orElse(null);

            entity.setPriceFluctuation(calculateFluctuation(previousPrice, entity.getPrice()));

            if (entity.getPrice() != null) {
                lastKnownPriceBySymbol.put(symbol, entity.getPrice());
            }
        }
    }

    private Double calculateFluctuation(Double previousPrice, Double currentPrice) {
        if (previousPrice == null || currentPrice == null || previousPrice == 0) {
            return null;
        }
        return ((currentPrice - previousPrice) / previousPrice) * 100;
    }
}
