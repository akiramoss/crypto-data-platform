package com.crypto_data_platform.crypto_data_platform.service;

import com.crypto_data_platform.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private final CryptoApiClient apiClient;
    private final CryptoRepository repository;

    public CryptoService(CryptoApiClient apiClient, CryptoRepository repository) {
        this.apiClient = apiClient;
        this.repository = repository;
    }

    public void fetchAndSaveCryptoData() {
        CryptoApiResponse[] response = apiClient.fetchCryptoData();

        for (CryptoApiResponse dto : response) {

            CryptoPrice entity = new CryptoPrice();

            entity.setSymbol(dto.getSymbol());
            entity.setPrice(dto.getCurrent_price());
            entity.setMarketCap(dto.getMarket_cap());
            entity.setVolume(dto.getTotal_volume());
            entity.setTimestamp(System.currentTimeMillis());

            repository.save(entity);
        }
    }

}
