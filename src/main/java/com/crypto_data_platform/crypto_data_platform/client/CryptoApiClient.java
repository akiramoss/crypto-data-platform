package com.crypto_data_platform.crypto_data_platform.client;

import com.crypto_data_platform.crypto_data_platform.config.CryptoApiConfig;
import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CryptoApiClient {

    private static final Logger logger = LoggerFactory.getLogger(CryptoApiClient.class);

    private final RestTemplate restTemplate;
    private final CryptoApiConfig config;

    public CryptoApiClient(CryptoApiConfig config) {
        this.restTemplate = new RestTemplate();
        this.config = config;
    }

    public CryptoApiResponse[] fetchCryptoData() {
        String url = config.getUrl()
                + "?vs_currency=" + config.getVsCurrency()
                + "&order=" + config.getOrder()
                + "&per_page=" + config.getPerPage();

        logger.info("Calling API with URL: {}", url);

        try {
            CryptoApiResponse[] response = restTemplate.getForObject(url, CryptoApiResponse[].class);

            logger.info("API call successful, received {} records",
                    response != null ? response.length : 0);

            return response;

        } catch (Exception e) {
            logger.error("Error calling crypto API", e);
            throw e;
        }
    }
}
