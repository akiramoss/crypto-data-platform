package com.crypto_data_platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NdjsonFileWriter is package-private in src/main/java/.../service, so this test lives in the
 * same package (under src/test/java) to be able to instantiate and call it directly, using a
 * @TempDir instead of the hardcoded "data/raw" / "data/processed" directories used by
 * RawDataService / ProcessedDataService.
 */
class NdjsonFileWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final NdjsonFileWriter writer = new NdjsonFileWriter(objectMapper);

    private record Sample(String symbol, double price, LocalDateTime eventTime) {
    }

    @Test
    void write_createsOneJsonLinePerItem_withDateFieldsSerialized(@TempDir Path tempDir) throws IOException {
        // Arrange
        List<Sample> items = List.of(
                new Sample("BTC", 65000.5, LocalDateTime.of(2024, 1, 15, 10, 30)),
                new Sample("ETH", 3200.1, LocalDateTime.of(2024, 1, 15, 10, 31))
        );

        // Act
        String fileName = writer.write(tempDir.toString(), "crypto_", items);

        // Assert
        Path writtenFile = Path.of(fileName);
        assertThat(writtenFile).exists();
        List<String> lines = Files.readAllLines(writtenFile);
        assertThat(lines).hasSize(2);

        Map<String, Object> firstLine = objectMapper.readValue(lines.get(0), new TypeReference<>() {
        });
        assertThat(firstLine).containsEntry("symbol", "BTC");
        assertThat(firstLine).containsKey("eventTime");

        Map<String, Object> secondLine = objectMapper.readValue(lines.get(1), new TypeReference<>() {
        });
        assertThat(secondLine).containsEntry("symbol", "ETH");
    }

    @Test
    void write_createsDirectory_whenItDoesNotExistYet(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path nested = tempDir.resolve("nested/does/not/exist/yet");
        assertThat(nested).doesNotExist();

        // Act
        writer.write(nested.toString(), "crypto_", List.of(new Sample("BTC", 1.0, LocalDateTime.now())));

        // Assert
        assertThat(nested).exists().isDirectory();
    }

    @Test
    void write_returnsPathOfFileThatWasActuallyCreated(@TempDir Path tempDir) throws IOException {
        // Act
        String fileName = writer.write(tempDir.toString(), "myprefix_", List.of());

        // Assert
        assertThat(Path.of(fileName)).exists();
        assertThat(Path.of(fileName).getFileName().toString()).startsWith("myprefix_");
    }

    @Test
    void write_writesEmptyFile_whenItemsCollectionIsEmpty(@TempDir Path tempDir) throws IOException {
        // Act
        String fileName = writer.write(tempDir.toString(), "crypto_", List.of());

        // Assert
        assertThat(Files.readAllLines(Path.of(fileName))).isEmpty();
    }

    /** POJO whose Jackson serialization always fails, to simulate a broken item mid-batch. */
    static class Unserializable {
        public String getValue() {
            throw new RuntimeException("boom: cannot serialize this field");
        }
    }

    @Test
    void write_throwsIOException_andCreatesNoFileAtAll_whenAnItemFailsToSerialize(@TempDir Path tempDir) {
        // Fixed: NdjsonFileWriter.write(...) (service/NdjsonFileWriter.java) now serializes every
        // item BEFORE opening/creating the output file. First item is fine, second item blows up
        // during serialization.
        List<Object> items = List.of(new Sample("BTC", 1.0, LocalDateTime.now()), new Unserializable());

        // Act + Assert: exception propagates out of write() (declared as throws IOException).
        assertThatThrownBy(() -> writer.write(tempDir.toString(), "crypto_", items))
                .isInstanceOf(IOException.class);

        // No partial/truncated file is left behind: the failure happens before any file is created.
        File[] createdFiles = tempDir.toFile().listFiles();
        assertThat(createdFiles).isNotNull().isEmpty();
    }
}
