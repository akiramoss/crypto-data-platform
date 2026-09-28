package com.crypto_data_platform;

import com.crypto_data_platform.service.CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the Actuator health endpoint, boots the full context on the "test" profile
 * (H2 in-memory DB, see application-test.properties) so the DB health indicator has something
 * real (if in-memory) to check against, with no real MySQL instance required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorHealthTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    // Prevents the scheduler from making a real CoinGecko call during this test.
    @MockBean
    private CryptoService cryptoService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void health_returnsOkWithUpStatus_andIncludesDbCheck() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
        // management.endpoint.health.show-details=always exposes per-component detail,
        // including the auto-configured DataSource ("db") health indicator.
        assertThat(response.getBody()).contains("\"db\"");
    }
}
