package com.crypto_data_platform.crypto_data_platform.client;

import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CryptoApiClient {

    // HTTP Client (desacoplar clases)
    // Change API, easy tests
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd";

    public CryptoApiResponse[] fetchCrytoData() {
        return restTemplate.getForObject(API_URL, CryptoApiResponse[].class);
    }
}
