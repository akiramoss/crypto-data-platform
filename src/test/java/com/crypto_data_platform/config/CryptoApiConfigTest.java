package com.crypto_data_platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code @ConfigurationProperties(prefix = "crypto.api")} binding on
 * {@link CryptoApiConfig} in isolation (no full Spring Boot context needed), using
 * {@link ApplicationContextRunner}. In particular, checks that every field binds from its
 * expected property name, including the {@code apiKey} field which binds from the
 * kebab-case "crypto.api.api-key" (relaxed binding), not from a literal "crypto.api.apiKey".
 */
class CryptoApiConfigTest {

    @EnableConfigurationProperties(CryptoApiConfig.class)
    static class TestConfiguration {
    }

    @Test
    void bindsAllFields_fromCryptoApiPrefixedProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
                .withPropertyValues(
                        "crypto.api.url=https://api.coingecko.com/api/v3/coins/markets",
                        "crypto.api.vsCurrency=usd",
                        "crypto.api.order=market_cap_desc",
                        "crypto.api.perPage=10",
                        "crypto.api.api-key=abc123"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    CryptoApiConfig config = context.getBean(CryptoApiConfig.class);
                    assertThat(config.getUrl()).isEqualTo("https://api.coingecko.com/api/v3/coins/markets");
                    assertThat(config.getVsCurrency()).isEqualTo("usd");
                    assertThat(config.getOrder()).isEqualTo("market_cap_desc");
                    assertThat(config.getPerPage()).isEqualTo(10);
                    // The Java field is "apiKey", but relaxed binding maps it from the
                    // kebab-case property "crypto.api.api-key" (see application.properties /
                    // application-test.properties, both of which use this exact kebab-case form).
                    assertThat(config.getApiKey()).isEqualTo("abc123");
                });
    }

    @Test
    void apiKey_isNull_whenNotConfigured_ratherThanEmptyString() {
        // Documents current behavior: CryptoApiConfig has no default value for apiKey, so when the
        // property is absent entirely (not even an empty string), the field stays null. This matters
        // because CryptoApiClient.buildHeaders() null-checks apiKey before calling isBlank() on it;
        // if that null-check were ever removed, this is the exact situation that would NPE.
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
                .withPropertyValues(
                        "crypto.api.url=https://api.coingecko.com/api/v3/coins/markets",
                        "crypto.api.vsCurrency=usd",
                        "crypto.api.order=market_cap_desc",
                        "crypto.api.perPage=10"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    CryptoApiConfig config = context.getBean(CryptoApiConfig.class);
                    assertThat(config.getApiKey()).isNull();
                });
    }

    @Test
    void perPage_failsToBind_whenValueIsNotAnInteger() {
        // perPage is a primitive int: a non-numeric value must fail context startup with a clear
        // binding error rather than silently defaulting to 0, which would otherwise send
        // "per_page=0" to CoinGecko (a plausible misconfiguration, e.g. a stray environment
        // variable typo) without any indication something is wrong.
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
                .withPropertyValues(
                        "crypto.api.url=https://api.coingecko.com/api/v3/coins/markets",
                        "crypto.api.vsCurrency=usd",
                        "crypto.api.order=market_cap_desc",
                        "crypto.api.perPage=not-a-number"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
