package com.crypto_data_platform.service;

import com.crypto_data_platform.dto.CryptoApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RawDataService writes to a hardcoded "data/raw" directory (relative to the JVM working
 * directory), so it can't be redirected to a @TempDir without touching production code.
 * These tests exercise the real directory but always clean up any file they create, so the
 * repository is left untouched after the run.
 */
class RawDataServiceTest {

    private static final String RAW_DIR = "data/raw";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RawDataService rawDataService = new RawDataService(objectMapper);

    private Set<String> filesBefore;

    @BeforeEach
    void snapshotDirectory() {
        filesBefore = listFiles();
    }

    @AfterEach
    void cleanupCreatedFiles() {
        Set<String> filesAfter = listFiles();
        filesAfter.removeAll(filesBefore);
        for (String created : filesAfter) {
            new File(RAW_DIR, created).delete();
        }
        // Remove the "data/raw" and "data" directories again if this test run created them and
        // they are now empty, so the repository is left exactly as it was found.
        File rawDir = new File(RAW_DIR);
        if (rawDir.exists() && rawDir.isDirectory() && rawDir.list() != null && rawDir.list().length == 0) {
            rawDir.delete();
            File dataDir = rawDir.getParentFile();
            if (dataDir != null && dataDir.isDirectory() && dataDir.list() != null && dataDir.list().length == 0) {
                dataDir.delete();
            }
        }
    }

    private Set<String> listFiles() {
        File dir = new File(RAW_DIR);
        String[] names = dir.exists() ? dir.list() : null;
        return names == null ? new HashSet<>() : new HashSet<>(List.of(names));
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
    void saveRawData_writesOneJsonLinePerEntity() throws IOException {
        // Arrange
        CryptoApiResponse[] data = {response("BTC", 65000.5), response("ETH", 3200.1)};

        // Act
        rawDataService.saveRawData(data);

        // Assert
        String createdFileName = newlyCreatedFile();
        List<String> lines = Files.readAllLines(Path.of(RAW_DIR, createdFileName));
        assertThat(lines).hasSize(2);

        Map<String, Object> firstLine = objectMapper.readValue(lines.get(0), new TypeReference<>() {
        });
        assertThat(firstLine).containsEntry("symbol", "BTC");
        assertThat(firstLine).containsEntry("last_updated", "2024-01-15T10:30:00.000Z");
    }

    @Test
    void saveRawData_throwsNullPointerException_whenDataArrayIsNull() {
        // BUG (documented, not fixed): RawDataService.saveRawData(Object[] data) (service/RawDataService.java:24-31)
        // calls Arrays.asList(data). When "data" is a null array reference (e.g. because
        // CryptoApiClient.fetchCryptoData() returned null), Arrays.asList(null) throws a
        // NullPointerException, which is NOT an IOException, so RawDataService's
        // catch (IOException e) does not catch it: the exception propagates to the caller
        // (CryptoService), and no RAW file at all is written for this fetch cycle.
        assertThatThrownBy(() -> rawDataService.saveRawData(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void saveRawData_swallowsSerializationFailure_andWritesPartialFile() throws IOException {
        // Arrange: second element cannot be serialized by Jackson.
        Object[] data = {response("BTC", 65000.5), new Object() {
            public String getBroken() {
                throw new RuntimeException("boom");
            }
        }};

        // Act: the IOException raised by the writer is caught inside saveRawData and only logged.
        assertThatCode(() -> rawDataService.saveRawData(data)).doesNotThrowAnyException();

        // Assert: a file WAS created, but only contains the line(s) written before the failure —
        // i.e. a partial/incomplete RAW file is silently produced with no signal to the caller.
        String createdFileName = newlyCreatedFile();
        List<String> lines = Files.readAllLines(Path.of(RAW_DIR, createdFileName));
        assertThat(lines).hasSize(1);
    }

    private String newlyCreatedFile() {
        Set<String> filesAfter = listFiles();
        filesAfter.removeAll(filesBefore);
        assertThat(filesAfter).hasSize(1);
        return filesAfter.iterator().next();
    }
}
