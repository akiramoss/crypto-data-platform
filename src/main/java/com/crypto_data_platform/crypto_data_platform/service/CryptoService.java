package com.crypto_data_platform.crypto_data_platform.service;

import com.crypto_data_platform.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);

    private final CryptoApiClient apiClient;
    private final CryptoRepository repository;
    private final List<CryptoPrice> entities = new ArrayList<>();

    public CryptoService(CryptoApiClient apiClient, CryptoRepository repository) {
        this.apiClient = apiClient;
        this.repository = repository;
    }

    public void fetchAndSaveCryptoData() {
        try {
            CryptoApiResponse[] response = apiClient.fetchCryptoData();

            logger.info("Fetched {} records from API", response.length);

            for (CryptoApiResponse dto : response) {

                // Evitar duplicados
                if (repository.existsBySymbol(dto.getSymbol())) {
                    continue;
                }

                CryptoPrice entity = new CryptoPrice();

                entity.setSymbol(dto.getSymbol());
                entity.setPrice(dto.getCurrent_price());
                entity.setMarketCap(dto.getMarket_cap());
                entity.setVolume(dto.getTotal_volume());
                entity.setTimestamp(System.currentTimeMillis());

                // Procesar datos en batch (lotes)
                // 100 registros --> 1 batch insert
                entities.add(entity);
            }

            logger.info("Saving {} new entities", entities.size());

            repository.saveAll(entities);

            logger.info("Data ingestion completed");
            // API --> DTO --> Service --> Batch --> DB

        }catch(Exception e) {
            logger.error("ERROR during crypto ingestion:", e);
            e.printStackTrace();
        }
    }
}
