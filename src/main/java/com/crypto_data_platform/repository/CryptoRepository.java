package com.crypto_data_platform.repository;

import com.crypto_data_platform.domain.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CryptoRepository extends JpaRepository<CryptoPrice, Long> {

    boolean existsBySymbolAndEventTime(String symbol, LocalDateTime eventTime);
}
