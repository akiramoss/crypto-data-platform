package com.crypto_data_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper compartido para escribir colecciones de objetos en formato NDJSON
 * (una línea JSON por objeto). Usado tanto por {@link RawDataService} como
 * por {@link ProcessedDataService} para evitar duplicar la lógica de
 * creación de directorios, nombrado de ficheros y escritura línea a línea.
 */
class NdjsonFileWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ObjectMapper objectMapper;

    NdjsonFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Escribe los elementos dados como NDJSON en un fichero nuevo dentro del
     * directorio indicado, con el prefijo de nombre dado y un timestamp.
     *
     * @param directoryPath carpeta donde se guardará el fichero (se crea si no existe)
     * @param fileNamePrefix prefijo del nombre de fichero (ej. "crypto_")
     * @param items          elementos a serializar, uno por línea
     * @return la ruta del fichero escrito
     */
    String write(String directoryPath, String fileNamePrefix, Iterable<?> items) throws IOException {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String fileName = directoryPath + "/" + fileNamePrefix + timestamp + ".json";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Object item : items) {
                String jsonLine = objectMapper.writeValueAsString(item);
                writer.write(jsonLine);
                writer.newLine();
            }
        }

        return fileName;
    }
}
