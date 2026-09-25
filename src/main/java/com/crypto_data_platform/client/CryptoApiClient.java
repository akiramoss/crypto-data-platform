package com.crypto_data_platform.client;

import com.crypto_data_platform.config.CryptoApiConfig;
import com.crypto_data_platform.dto.CryptoApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CryptoApiClient {

    private static final String API_KEY_HEADER = "x-cg-demo-api-key";

    private static final Logger logger = LoggerFactory.getLogger(CryptoApiClient.class);

    private final RestTemplate restTemplate;
    private final CryptoApiConfig config;

    public CryptoApiClient(RestTemplate restTemplate, CryptoApiConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public CryptoApiResponse[] fetchCryptoData() {
        String url = config.getUrl()
                + "?vs_currency=" + config.getVsCurrency()
                + "&order=" + config.getOrder()
                + "&per_page=" + config.getPerPage();

        logger.info("Calling API with URL: {}", url);

        HttpHeaders headers = new HttpHeaders();
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set(API_KEY_HEADER, apiKey);
        }

        try {
            CryptoApiResponse[] response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), CryptoApiResponse[].class).getBody();

            logger.info("API call successful, received {} records",
                    response != null ? response.length : 0);

            return response;

        } catch (Exception e) {
            logger.error("Error calling crypto API", e);
            throw e;
        }
    }
}
