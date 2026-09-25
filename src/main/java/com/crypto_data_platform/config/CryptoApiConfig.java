package com.crypto_data_platform.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
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
}
