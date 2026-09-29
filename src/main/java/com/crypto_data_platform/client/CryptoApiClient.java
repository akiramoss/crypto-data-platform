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

    // CoinGecko sirve su API pública tras CloudFront, que bloquea con 403 las peticiones cuyo
    // User-Agent delata un cliente HTTP por defecto (p.ej. "Java/17...", el que pone
    // HttpURLConnection si no se fija ninguno). Un User-Agent propio evita ese bloqueo de borde.
    private static final String USER_AGENT_HEADER = "crypto-data-platform/1.0";

    private static final Logger logger = LoggerFactory.getLogger(CryptoApiClient.class);

    private final RestTemplate restTemplate;
    private final CryptoApiConfig config;

    public CryptoApiClient(RestTemplate restTemplate, CryptoApiConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public CryptoApiResponse[] fetchCryptoData() {
        String url = buildRequestUrl();

        logger.info("Calling API with URL: {}", url);

        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        // Cualquier RestClientException se propaga tal cual: CryptoService ya la captura y
        // registra en el punto donde se decide qué hacer con el fallo, evitando loguear el
        // mismo error dos veces.
        CryptoApiResponse[] response = restTemplate.exchange(
                url, HttpMethod.GET, request, CryptoApiResponse[].class).getBody();

        logger.info("API call successful, received {} records",
                response != null ? response.length : 0);

        return response;
    }

    private String buildRequestUrl() {
        return config.getUrl()
                + "?vs_currency=" + config.getVsCurrency()
                + "&order=" + config.getOrder()
                + "&per_page=" + config.getPerPage();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT_HEADER);
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set(API_KEY_HEADER, apiKey);
        }
        return headers;
    }
}
