package com.crypto_data_platform.client;

import com.crypto_data_platform.config.CryptoApiConfig;
import com.crypto_data_platform.dto.CryptoApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * CryptoApiClient builds its own RestTemplate internally (it is not injected), so tests replace
 * that internal instance via reflection with one bound to a MockRestServiceServer. This lets us
 * simulate CoinGecko responses (success, HTTP errors, empty body) without any real network call.
 */
class CryptoApiClientTest {

    private CryptoApiConfig config;
    private CryptoApiClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        config = new CryptoApiConfig();
        ReflectionTestUtils.setField(config, "url", "http://fake-coingecko.test/api/v3/coins/markets");
        ReflectionTestUtils.setField(config, "vsCurrency", "usd");
        ReflectionTestUtils.setField(config, "order", "market_cap_desc");
        ReflectionTestUtils.setField(config, "perPage", 10);
        ReflectionTestUtils.setField(config, "apiKey", "test-key");

        client = new CryptoApiClient(config);

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
    }

    @Test
    void fetchCryptoData_buildsUrlFromConfig_andParsesSuccessfulResponse() {
        // Arrange
        String expectedUrl = "http://fake-coingecko.test/api/v3/coins/markets"
                + "?vs_currency=usd&order=market_cap_desc&per_page=10";
        String body = "["
                + "{\"id\":\"bitcoin\",\"symbol\":\"btc\",\"name\":\"Bitcoin\","
                + "\"current_price\":65000.5,\"market_cap\":1200000000,\"total_volume\":50000000,"
                + "\"last_updated\":\"2024-01-15T10:30:00.000Z\"}"
                + "]";
        server.expect(requestTo(expectedUrl))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // Act
        CryptoApiResponse[] result = client.fetchCryptoData();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result[0].getSymbol()).isEqualTo("btc");
        assertThat(result[0].getCurrent_price()).isEqualTo(65000.5);
        server.verify();
    }

    @Test
    void fetchCryptoData_returnsEmptyArray_whenApiRespondsWithEmptyJsonArray() {
        // Arrange
        server.expect(requestTo(org.hamcrest.Matchers.containsString("coins/markets")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // Act
        CryptoApiResponse[] result = client.fetchCryptoData();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void fetchCryptoData_throwsHttpServerErrorException_onHttp5xxResponse() {
        // Arrange
        server.expect(requestTo(org.hamcrest.Matchers.containsString("coins/markets")))
                .andRespond(withServerError());

        // Act + Assert: caught, logged, and rethrown by CryptoApiClient.fetchCryptoData().
        assertThatThrownBy(() -> client.fetchCryptoData())
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void fetchCryptoData_throwsHttpClientErrorException_onHttp4xxResponse() {
        // Arrange: e.g. a rate-limit (429) or unauthorized (401) response from CoinGecko.
        server.expect(requestTo(org.hamcrest.Matchers.containsString("coins/markets")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // Act + Assert
        assertThatThrownBy(() -> client.fetchCryptoData())
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void fetchCryptoData_neverSendsApiKey_inRequestUrl() {
        // NOTE: CryptoApiConfig exposes an apiKey (crypto.api.key), but CryptoApiClient.fetchCryptoData()
        // (client/CryptoApiClient.java:24-27) never includes it in the request URL or headers. This is
        // worth flagging: any CoinGecko plan requiring an API key would receive unauthenticated/rate
        // limited requests in production, even though the key is configured.
        String expectedUrl = "http://fake-coingecko.test/api/v3/coins/markets"
                + "?vs_currency=usd&order=market_cap_desc&per_page=10";
        server.expect(requestTo(expectedUrl)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.fetchCryptoData();

        server.verify();
    }
}
