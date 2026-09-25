package com.crypto_data_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

@Service
public class RawDataService {

    private static final Logger logger = LoggerFactory.getLogger(RawDataService.class);
    private static final String RAW_DATA_DIRECTORY = "data/raw";
    private static final String FILE_NAME_PREFIX = "crypto_";

    private final NdjsonFileWriter ndjsonFileWriter;

    public RawDataService(ObjectMapper objectMapper) {
        this.ndjsonFileWriter = new NdjsonFileWriter(objectMapper);
    }

    public void saveRawData(Object[] data) {
        if (data == null) {
            logger.warn("No raw data to save (null array), skipping RAW file write");
            return;
        }

        try {
            String fileName = ndjsonFileWriter.write(RAW_DATA_DIRECTORY, FILE_NAME_PREFIX, Arrays.asList(data));
            logger.info("RAW NDJSON saved at: {}", fileName);
        } catch (IOException e) {
            logger.error("Error saving raw data: {}", e.getMessage(), e);
        }
    }
}
