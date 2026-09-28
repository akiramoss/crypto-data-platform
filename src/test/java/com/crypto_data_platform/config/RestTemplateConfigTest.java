package com.crypto_data_platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigTest {

    @Test
    void restTemplate_hasConnectAndReadTimeoutsConfigured_soAHangingApiFailsFastInsteadOfBlockingForever() {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate(new RestTemplateBuilder());

        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();
        int connectTimeout = (int) ReflectionTestUtils.getField(factory, "connectTimeout");
        int readTimeout = (int) ReflectionTestUtils.getField(factory, "readTimeout");

        // -1 (the SimpleClientHttpRequestFactory/JDK default) means "no timeout", i.e. block
        // indefinitely - exactly what CryptoApiClient must never do against a hanging CoinGecko.
        assertThat(connectTimeout).isGreaterThan(0);
        assertThat(readTimeout).isGreaterThan(0);
    }
}
