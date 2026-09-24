package com.crypto_data_platform.service;

import com.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.repository.CryptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for CryptoService: no Spring context, no real DB, no real API.
 */
@ExtendWith(MockitoExtension.class)
class CryptoServiceTest {

    // See CryptoDataPlatformApplicationTests for why this is needed under this JDK.
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private CryptoApiClient apiClient;
    @Mock
    private CryptoRepository repository;
    @Mock
    private RawDataService rawDataService;
    @Mock
    private ProcessedDataService processedDataService;

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService(apiClient, repository, rawDataService, processedDataService);
    }

    private CryptoApiResponse response(String symbol, double price) {
        CryptoApiResponse dto = new CryptoApiResponse();
        dto.setId(symbol.toLowerCase());
        dto.setSymbol(symbol);
        dto.setName(symbol);
        dto.setCurrent_price(price);
        dto.setMarket_cap(price * 1000);
        dto.setTotal_volume(price * 10);
        dto.setLast_updated("2024-01-15T10:30:00.000Z");
        return dto;
    }

    @Test
    void fetchAndSaveCryptoData_happyPath_savesRawMapsAndPersistsAndSavesProcessed() {
        // Arrange
        CryptoApiResponse[] apiResponse = {response("BTC", 65000.5), response("ETH", 3200.1)};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);

        // Act
        cryptoService.fetchAndSaveCryptoData();

        // Assert
        verify(rawDataService).saveRawData(apiResponse);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CryptoPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<CryptoPrice> persisted = captor.getValue();
        assertThat(persisted).hasSize(2);
        assertThat(persisted.get(0).getSymbol()).isEqualTo("BTC");
        assertThat(persisted.get(0).getPrice()).isEqualTo(65000.5);
        assertThat(persisted.get(1).getSymbol()).isEqualTo("ETH");

        verify(processedDataService).saveProcessedData(persisted);
    }

    @Test
    void fetchAndSaveCryptoData_apiReturnsNull_abortsPipelineWithoutPersistingAnything() {
        // BUG (documented, not fixed): CryptoService.fetchFromApiAndSaveRaw() (service/CryptoService.java:52-60)
        // passes a possibly-null "response" straight to rawDataService.saveRawData(response), then
        // immediately calls "response.length" in the log statement on the next line. If the API
        // client returns null (e.g. CoinGecko outage returning an empty body), that logger.info call
        // throws a NullPointerException. It is swallowed by the top-level try/catch in
        // fetchAndSaveCryptoData(), so the failure is only visible in the logs — nothing is
        // persisted, and the caller (the scheduler) never finds out anything went wrong.
        when(apiClient.fetchCryptoData()).thenReturn(null);

        // Act: the outer catch(Exception) swallows the NPE, so the call itself must not throw.
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert
        verify(rawDataService).saveRawData(null);
        verify(repository, never()).saveAll(anyList());
        verify(processedDataService, never()).saveProcessedData(any());
    }

    @Test
    void fetchAndSaveCryptoData_apiReturnsEmptyArray_completesAndPersistsEmptyBatch() {
        // Arrange
        CryptoApiResponse[] emptyResponse = new CryptoApiResponse[0];
        when(apiClient.fetchCryptoData()).thenReturn(emptyResponse);

        // Act
        cryptoService.fetchAndSaveCryptoData();

        // Assert: pipeline completes normally, calling downstream collaborators with empty data.
        verify(rawDataService).saveRawData(emptyResponse);
        verify(repository).saveAll(List.of());
        verify(processedDataService).saveProcessedData(List.of());
    }

    @Test
    void fetchAndSaveCryptoData_oneInvalidLastUpdatedInBatch_dropsWholeBatchSilently() {
        // BUG (documented, not fixed): CryptoService.mapToEntities() (service/CryptoService.java:62-71)
        // calls CryptoMapper.toEntity(dto) in a plain loop with no per-item try/catch. If ANY item
        // in the batch has an invalid/null "last_updated" (see CryptoMapperTest), the mapper throws
        // and the exception propagates up through fetchAndSaveCryptoData()'s top-level catch. As a
        // result, valid entries earlier in the same batch (BTC below) are silently discarded too:
        // nothing is persisted for the entire fetch cycle, and only a log line records the failure.
        CryptoApiResponse valid = response("BTC", 65000.5);
        CryptoApiResponse invalid = response("ETH", 3200.1);
        invalid.setLast_updated("not-a-valid-date");
        CryptoApiResponse[] apiResponse = {valid, invalid};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert: raw data for the whole (unparsed) batch is still saved...
        verify(rawDataService).saveRawData(apiResponse);
        // ...but NOTHING is persisted to the DB or written to PROCESSED, not even the valid BTC entry.
        verify(repository, never()).saveAll(anyList());
        verify(processedDataService, never()).saveProcessedData(any());
    }

    @Test
    void fetchAndSaveCryptoData_repositorySaveAllThrowsOnDuplicate_processedDataNeverSaved() {
        // BUG (documented, not fixed): CryptoService.persistEntitiesAndProcessedCopy() (service/CryptoService.java:73-81)
        // wraps repository.saveAll(entities) and processedDataService.saveProcessedData(entities) in
        // a single try/catch. Spring Data JPA's saveAll is effectively all-or-nothing for a batch
        // (a single duplicate/constraint violation aborts the whole saveAll call), so ONE duplicate
        // record in the batch causes the ENTIRE batch to be lost — none of the (possibly many other
        // valid, non-duplicate) entities get persisted — and the PROCESSED copy is skipped
        // altogether for that fetch cycle, since saveProcessedData(...) is never reached. The
        // failure is only logged as a generic warning, with no per-item recovery or retry.
        CryptoApiResponse[] apiResponse = {response("BTC", 65000.5), response("ETH", 3200.1), response("SOL", 140.0)};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);
        doThrow(new DataIntegrityViolationException("Duplicate entry for key (symbol, event_time)"))
                .when(repository).saveAll(anyList());

        // Act: exception is caught and logged inside persistEntitiesAndProcessedCopy/fetchAndSaveCryptoData.
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert
        verify(repository).saveAll(anyList());
        verify(processedDataService, never()).saveProcessedData(any());
    }

    @Test
    void fetchAndSaveCryptoData_apiClientThrows_noDownstreamCollaboratorIsCalled() {
        // Arrange: simulate a network/HTTP failure surfaced by CryptoApiClient (see CryptoApiClientTest).
        when(apiClient.fetchCryptoData())
                .thenThrow(new RuntimeException("simulated API failure"));

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert: nothing downstream of the failed API call is ever invoked.
        verifyNoInteractions(rawDataService, repository, processedDataService);
    }
}
