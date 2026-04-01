package com.crypto_data_platform.crypto_data_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RawDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveRawData(Object data) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            String fileName = "data/raw/crypto_" + timestamp + ".json";

            objectMapper.writeValue(new File(fileName), data);

        } catch (Exception e) {
            System.out.println("Error saving raw data: " + e.getMessage());
        }
    }
}
