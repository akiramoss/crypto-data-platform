package com.crypto_data_platform.service;

import com.crypto_data_platform.domain.CryptoPrice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ProcessedDataService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessedDataService.class);
    private static final String PROCESSED_DATA_DIRECTORY = "data/processed";
    private static final String FILE_NAME_PREFIX = "crypto_processed_";

    private final NdjsonFileWriter ndjsonFileWriter;

    public ProcessedDataService(ObjectMapper objectMapper) {
        this.ndjsonFileWriter = new NdjsonFileWriter(objectMapper);
    }

    public void saveProcessedData(List<CryptoPrice> data) {
        if (data == null) {
            logger.warn("No processed data to save (null list), skipping PROCESSED file write");
            return;
        }

        try {
            String fileName = ndjsonFileWriter.write(PROCESSED_DATA_DIRECTORY, FILE_NAME_PREFIX, data);
            logger.info("PROCESSED data saved at: {}", fileName);
        } catch (IOException e) {
            logger.error("Error saving processed data: {}", e.getMessage(), e);
        }
    }
}
