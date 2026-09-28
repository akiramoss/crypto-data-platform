package com.crypto_data_platform.repository;

import com.crypto_data_platform.domain.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CryptoRepository extends JpaRepository<CryptoPrice, Long> {

    boolean existsBySymbolAndEventTime(String symbol, LocalDateTime eventTime);

    Optional<CryptoPrice> findTopBySymbolOrderByEventTimeDesc(String symbol);

    List<CryptoPrice> findBySymbolOrderByEventTimeDesc(String symbol);

    @Query("SELECT c.symbol AS symbol, COUNT(c) AS totalCount, "
            + "SUM(CASE WHEN c.priceFluctuation > 0 THEN 1L ELSE 0L END) AS positiveCount "
            + "FROM CryptoPrice c WHERE c.priceFluctuation IS NOT NULL GROUP BY c.symbol")
    List<SymbolFluctuationStats> aggregateFluctuationStatsBySymbol();

    interface SymbolFluctuationStats {
        String getSymbol();

        Long getTotalCount();

        Long getPositiveCount();
    }
}
