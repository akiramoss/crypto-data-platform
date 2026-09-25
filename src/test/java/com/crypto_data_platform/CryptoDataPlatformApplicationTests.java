package com.crypto_data_platform;

import com.crypto_data_platform.service.CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that boots the full Spring context using the "test" profile
 * (H2 in-memory database, see application-test.properties) instead of the
 * real MySQL instance configured in application.properties.
 * <p>
 * {@link CryptoService} is replaced by a Mockito mock so that, even though
 * {@code @EnableScheduling} is active and {@link com.crypto_data_platform.scheduler.CryptoScheduler}
 * may fire almost immediately after context startup, no real call to the
 * CoinGecko API and no real database write ever happens during this test.
 */
@SpringBootTest
@ActiveProfiles("test")
class CryptoDataPlatformApplicationTests {

    // NOTE: this JVM runs Java 25, newer than what the Byte Buddy version bundled with the
    // current Mockito/Hibernate releases officially supports (Java 23). Byte Buddy resolves and
    // caches this compatibility flag once per JVM the first time it is used, so every test class
    // that (directly or via Spring/Hibernate) triggers Byte Buddy must set this property in its
    // OWN static initializer, before its own usage, to be safe regardless of test run order.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @MockBean
    private CryptoService cryptoService;

    @Test
    void contextLoads(ApplicationContext context) {
        assertThat(context).isNotNull();
        assertThat(context.getBean(CryptoService.class)).isSameAs(cryptoService);
    }
}
