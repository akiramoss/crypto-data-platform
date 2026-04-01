package com.crypto_data_platform.crypto_data_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RawDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveRawData(Object[] data) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            File directory = new File("data/raw");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "data/raw/crypto_" + timestamp + ".json";

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            for (Object obj : data) {
                String jsonLine = objectMapper.writeValueAsString(obj);
                writer.write(jsonLine);
                writer.newLine();
            }

            writer.close();

            System.out.println("RAW NDJSON saved at: " + fileName);

        } catch (Exception e) {
            System.out.println("Error saving raw data: " + e.getMessage());
        }
    }
}
