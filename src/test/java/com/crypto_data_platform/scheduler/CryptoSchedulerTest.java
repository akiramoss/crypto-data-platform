package com.crypto_data_platform.scheduler;

import com.crypto_data_platform.service.CryptoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CryptoSchedulerTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private CryptoService cryptoService;

    @Test
    void runCryptoPipeline_invokesFetchAndSaveCryptoDataExactlyOnce() {
        // Arrange
        CryptoScheduler scheduler = new CryptoScheduler(cryptoService);

        // Act
        scheduler.runCryptoPipeline();

        // Assert
        verify(cryptoService, times(1)).fetchAndSaveCryptoData();
    }

    @Test
    void runCryptoPipeline_doesNotPropagateException_whenServiceThrows() {
        // Arrange
        doThrow(new RuntimeException("boom")).when(cryptoService).fetchAndSaveCryptoData();
        CryptoScheduler scheduler = new CryptoScheduler(cryptoService);

        // Act + Assert: CryptoScheduler.runCryptoPipeline() wraps the call in its own try/catch,
        // so a failure inside the service must never escape and break the @Scheduled trigger.
        assertThatCode(scheduler::runCryptoPipeline).doesNotThrowAnyException();
        verify(cryptoService, times(1)).fetchAndSaveCryptoData();
    }
}
