package com.crypto_data_platform.service;

import com.crypto_data_platform.domain.CryptoPrice;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ProcessedDataService writes to a hardcoded "data/processed" directory (relative to the JVM
 * working directory), so it can't be redirected to a @TempDir without touching production code.
 * These tests exercise the real directory but always clean up any file/dir they create.
 */
class ProcessedDataServiceTest {

    private static final String PROCESSED_DIR = "data/processed";

    // Mirrors the (now fixed) JacksonConfig.objectMapper() bean used in production.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ProcessedDataService processedDataService = new ProcessedDataService(objectMapper);

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
            new File(PROCESSED_DIR, created).delete();
        }
        File processedDir = new File(PROCESSED_DIR);
        if (processedDir.exists() && processedDir.isDirectory()
                && processedDir.list() != null && processedDir.list().length == 0) {
            processedDir.delete();
            File dataDir = processedDir.getParentFile();
            if (dataDir != null && dataDir.isDirectory() && dataDir.list() != null && dataDir.list().length == 0) {
                dataDir.delete();
            }
        }
    }

    private Set<String> listFiles() {
        File dir = new File(PROCESSED_DIR);
        String[] names = dir.exists() ? dir.list() : null;
        return names == null ? new HashSet<>() : new HashSet<>(List.of(names));
    }

    private CryptoPrice entity(String symbol, double price, LocalDateTime eventTime) {
        CryptoPrice entity = new CryptoPrice();
        entity.setSymbol(symbol);
        entity.setPrice(price);
        entity.setMarketCap(price * 1000);
        entity.setVolume(price * 10);
        entity.setEventTime(eventTime);
        entity.setTimeStamp(LocalDateTime.now());
        return entity;
    }

    @Test
    void saveProcessedData_writesOneJsonLinePerEntity_includingDateFields() throws IOException {
        // Arrange
        List<CryptoPrice> entities = List.of(
                entity("BTC", 65000.5, LocalDateTime.of(2024, 1, 15, 10, 30)),
                entity("ETH", 3200.1, LocalDateTime.of(2024, 1, 15, 10, 31))
        );

        // Act
        processedDataService.saveProcessedData(entities);

        // Assert
        String createdFileName = newlyCreatedFile();
        List<String> lines = Files.readAllLines(Path.of(PROCESSED_DIR, createdFileName));
        assertThat(lines).hasSize(2);

        Map<String, Object> firstLine = objectMapper.readValue(lines.get(0), new TypeReference<>() {
        });
        assertThat(firstLine).containsEntry("symbol", "BTC");
        assertThat(firstLine).containsKeys("eventTime", "timeStamp");
        // Fixed: JacksonConfig now disables SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, so
        // LocalDateTime fields serialize as a readable ISO-8601 string instead of a numeric array.
        assertThat(firstLine.get("eventTime")).isEqualTo("2024-01-15T10:30:00");
    }

    @Test
    void saveProcessedData_writesEmptyFile_whenEntityListIsEmpty() throws IOException {
        // Act
        processedDataService.saveProcessedData(List.of());

        // Assert: an empty NDJSON file is still created (documents current behavior for the
        // "API returned an empty array" scenario in the overall pipeline).
        String createdFileName = newlyCreatedFile();
        assertThat(Files.readAllLines(Path.of(PROCESSED_DIR, createdFileName))).isEmpty();
    }

    @Test
    void saveProcessedData_doesNotThrow_andWritesNoFile_whenDataListIsNull() {
        // Fixed: ProcessedDataService.saveProcessedData(List<CryptoPrice> data) (service/ProcessedDataService.java)
        // now null-checks "data" before handing it to NdjsonFileWriter.write(...), which used to
        // iterate a null list and throw an uncaught NullPointerException. Same class of fix as
        // RawDataService.saveRawData(null).
        assertThatCode(() -> processedDataService.saveProcessedData(null)).doesNotThrowAnyException();

        Set<String> filesAfter = listFiles();
        filesAfter.removeAll(filesBefore);
        assertThat(filesAfter).isEmpty();
    }

    private String newlyCreatedFile() {
        Set<String> filesAfter = listFiles();
        filesAfter.removeAll(filesBefore);
        assertThat(filesAfter).hasSize(1);
        return filesAfter.iterator().next();
    }
}
