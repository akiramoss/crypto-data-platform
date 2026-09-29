package com.crypto_data_platform.service;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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

    // Escala final de priceFluctuation (ver también @Column(scale = 4) en CryptoPrice); la
    // división intermedia usa MathContext.DECIMAL64 (16 dígitos significativos), de sobra para
    // no perder precisión antes de redondear al resultado que se persiste.
    private static final int FLUCTUATION_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

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
        Map<String, BigDecimal> lastKnownPriceBySymbol = new HashMap<>();

        for (CryptoPrice entity : entities) {
            String symbol = entity.getSymbol();
            BigDecimal previousPrice = lastKnownPriceBySymbol.containsKey(symbol)
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

    private BigDecimal calculateFluctuation(BigDecimal previousPrice, BigDecimal currentPrice) {
        if (previousPrice == null || currentPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentPrice.subtract(previousPrice)
                .divide(previousPrice, MathContext.DECIMAL64)
                .multiply(ONE_HUNDRED)
                .setScale(FLUCTUATION_SCALE, RoundingMode.HALF_UP);
    }
}
