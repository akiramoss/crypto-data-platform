package com.crypto_data_platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "crypto.api")
public class CryptoApiConfig {

    private String url;
    private String vsCurrency;
    private String order;
    private int perPage;
    private String apiKey;
}
