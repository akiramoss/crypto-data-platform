package com.crypto_data_platform.crypto_data_platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CryptoApiConfig {

    @Value("${crypto.api.url}")
    private String url;

    @Value("${crypto.api.vsCurrency}")
    private String vsCurrency;

    @Value("${crypto.api.order}")
    private String order;

    @Value("${crypto.api.perPage}")
    private int perPage;

    @Value("${crypto.api.key}")
    private String apiKey;

    public String getUrl() {
        return url;
    }

    public String getVsCurrency() {
        return vsCurrency;
    }

    public String getOrder() {
        return order;
    }

    public int getPerPage() {
        return perPage;
    }

    public String getApiKey(){
        return apiKey;
    }
}
