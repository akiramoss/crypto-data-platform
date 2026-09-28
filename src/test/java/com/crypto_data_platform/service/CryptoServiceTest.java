package com.crypto_data_platform.service;

import com.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.mapper.CryptoMapper;
import com.crypto_data_platform.repository.CryptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private PriceFluctuationService fluctuationService;

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        // CryptoMapper is pure/stateless, so a real instance is used here instead of a mock.
        cryptoService = new CryptoService(apiClient, repository, rawDataService, processedDataService, new CryptoMapper(), fluctuationService);
    }

    private CryptoApiResponse response(String symbol, double price) {
        CryptoApiResponse dto = new CryptoApiResponse();
        dto.setId(symbol.toLowerCase());
        dto.setSymbol(symbol);
        dto.setName(symbol);
        dto.setCurrentPrice(price);
        dto.setMarketCap(price * 1000);
        dto.setTotalVolume(price * 10);
        dto.setLastUpdated("2024-01-15T10:30:00.000Z");
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
    void fetchAndSaveCryptoData_apiReturnsNull_skipsRawSave_andCompletesCycleWithEmptyBatch() {
        // Fixed: CryptoService.fetchFromApiAndSaveRaw() (service/CryptoService.java) now null-checks
        // the API response before saving raw data or reading its length. A null response (e.g. a
        // CoinGecko outage returning an empty body) no longer produces an NPE swallowed by the
        // top-level catch; instead it logs a warning and the cycle completes gracefully with an
        // empty batch, just like the "API returns an empty array" case below.
        when(apiClient.fetchCryptoData()).thenReturn(null);

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert: raw data is never saved for a null response...
        verify(rawDataService, never()).saveRawData(any());
        // ...but the cycle still completes with an empty batch, same as an empty array response.
        verify(repository).saveAll(List.of());
        verify(processedDataService).saveProcessedData(List.of());
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
    void fetchAndSaveCryptoData_oneInvalidLastUpdatedInBatch_skipsOnlyThatRecord_persistsRestOfBatch() {
        // Fixed: CryptoService.mapToEntities() (service/CryptoService.java) now wraps
        // CryptoMapper.toEntity(dto) in a per-item try/catch. An item with an invalid/null
        // "last_updated" (see CryptoMapperTest) is logged and skipped instead of aborting the
        // mapping of the entire batch, so valid entries in the same batch (BTC below) are still
        // persisted.
        CryptoApiResponse valid = response("BTC", 65000.5);
        CryptoApiResponse invalid = response("ETH", 3200.1);
        invalid.setLastUpdated("not-a-valid-date");
        CryptoApiResponse[] apiResponse = {valid, invalid};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert: raw data for the whole (unparsed) batch is still saved...
        verify(rawDataService).saveRawData(apiResponse);

        // ...and the valid BTC entry is persisted and processed despite the malformed ETH record.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CryptoPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getSymbol()).isEqualTo("BTC");

        verify(processedDataService).saveProcessedData(captor.getValue());
    }

    @Test
    void fetchAndSaveCryptoData_oneEntityAlreadyExistsInDb_isFilteredOut_othersPersistedAndAllProcessed() {
        // Fixed: CryptoService.persistEntitiesAndProcessedCopy() (service/CryptoService.java) now
        // filters out entities that already exist (by symbol + eventTime) BEFORE calling
        // repository.saveAll(...), instead of letting a unique-constraint violation abort the whole
        // batch. A duplicate no longer causes the entire batch to be lost, and the PROCESSED copy is
        // always written for everything fetched this cycle, regardless of DB persistence outcome.
        CryptoApiResponse btc = response("BTC", 65000.5);
        CryptoApiResponse eth = response("ETH", 3200.1);
        CryptoApiResponse sol = response("SOL", 140.0);
        CryptoApiResponse[] apiResponse = {btc, eth, sol};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);
        // Simulate ETH already being present in the database for this eventTime (BTC/SOL are new).
        // All three symbols are stubbed explicitly to avoid Mockito's strict-stub argument-mismatch
        // check, since existsBySymbolAndEventTime is called once per entity in the batch.
        when(repository.existsBySymbolAndEventTime(eq("BTC"), any())).thenReturn(false);
        when(repository.existsBySymbolAndEventTime(eq("ETH"), any())).thenReturn(true);
        when(repository.existsBySymbolAndEventTime(eq("SOL"), any())).thenReturn(false);

        // Act
        cryptoService.fetchAndSaveCryptoData();

        // Assert: only BTC and SOL (not the duplicate ETH) are sent to the repository...
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CryptoPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(CryptoPrice::getSymbol).containsExactly("BTC", "SOL");

        // ...but the PROCESSED copy still contains all 3 entities fetched this cycle.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CryptoPrice>> processedCaptor = ArgumentCaptor.forClass(List.class);
        verify(processedDataService).saveProcessedData(processedCaptor.capture());
        assertThat(processedCaptor.getValue()).extracting(CryptoPrice::getSymbol)
                .containsExactly("BTC", "ETH", "SOL");
    }

    @Test
    void fetchAndSaveCryptoData_saveAllThrowsUnexpectedError_isLoggedButProcessedDataStillSaved() {
        // Fixed: an unexpected repository failure (unrelated to duplicates, e.g. a transient DB
        // error) is now caught on its own and no longer prevents the PROCESSED copy from being
        // saved, since saveProcessedData(...) is called unconditionally afterwards.
        // CryptoService only catches DataAccessException here (see CryptoService#persistEntitiesAndProcessedCopy),
        // matching what Spring Data repositories actually throw on a transient DB failure.
        CryptoApiResponse[] apiResponse = {response("BTC", 65000.5), response("ETH", 3200.1)};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);
        doThrow(new DataAccessResourceFailureException("simulated transient DB failure"))
                .when(repository).saveAll(anyList());

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert
        verify(repository).saveAll(anyList());
        verify(processedDataService).saveProcessedData(anyList());
    }

    @Test
    void fetchAndSaveCryptoData_intraBatchDuplicate_isDeduplicated_othersStillPersisted() {
        // Regression test: CryptoService.persistEntitiesAndProcessedCopy() (CryptoService.java) now
        // deduplicates by (symbol, eventTime) WITHIN the incoming batch, before checking against the
        // DB. Previously, two records sharing the same symbol+eventTime in a single API response
        // both passed the "not already in DB" pre-check and were sent to repository.saveAll(...)
        // together, so the (symbol, event_time) unique constraint would reject the WHOLE batch,
        // taking down unrelated valid entities (e.g. ETH below) with it. Now only the first of the
        // duplicate pair is kept, so saveAll(...) never sees a colliding pair and the rest of the
        // batch persists normally.
        CryptoApiResponse duplicateBtc1 = response("BTC", 65000.5);
        CryptoApiResponse duplicateBtc2 = response("BTC", 65000.5);
        CryptoApiResponse eth = response("ETH", 3200.1);
        CryptoApiResponse[] apiResponse = {duplicateBtc1, duplicateBtc2, eth};
        when(apiClient.fetchCryptoData()).thenReturn(apiResponse);
        when(repository.existsBySymbolAndEventTime(eq("BTC"), any())).thenReturn(false);
        when(repository.existsBySymbolAndEventTime(eq("ETH"), any())).thenReturn(false);

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert: only one BTC (the first of the duplicate pair) and the ETH entry reach saveAll...
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CryptoPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(CryptoPrice::getSymbol)
                .containsExactly("BTC", "ETH");

        // ...while the PROCESSED copy still reflects everything fetched this cycle, duplicates
        // included, since it documents what was fetched, not what was persisted.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CryptoPrice>> processedCaptor = ArgumentCaptor.forClass(List.class);
        verify(processedDataService).saveProcessedData(processedCaptor.capture());
        assertThat(processedCaptor.getValue()).extracting(CryptoPrice::getSymbol)
                .containsExactly("BTC", "BTC", "ETH");
    }

    @Test
    void fetchAndSaveCryptoData_apiClientThrows_noDownstreamCollaboratorIsCalled() {
        // Arrange: simulate a network/HTTP failure surfaced by CryptoApiClient (see CryptoApiClientTest).
        // CryptoService only catches RestClientException at the top level (see
        // CryptoService#fetchAndSaveCryptoData), matching what CryptoApiClient/RestTemplate actually throw.
        when(apiClient.fetchCryptoData())
                .thenThrow(new ResourceAccessException("simulated API failure"));

        // Act
        assertThatCode(() -> cryptoService.fetchAndSaveCryptoData()).doesNotThrowAnyException();

        // Assert: nothing downstream of the failed API call is ever invoked.
        verifyNoInteractions(rawDataService, repository, processedDataService);
    }
}
