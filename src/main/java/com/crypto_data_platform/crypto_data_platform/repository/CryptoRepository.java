package com.crypto_data_platform.crypto_data_platform.repository;

import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CryptoRepository extends JpaRepository<CryptoPrice, Long> {

}
