package com.crypto_data_platform.crypto_data_platform.service;

import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProcessedDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveProcessedData(List<CryptoPrice> data) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            File directory = new File("data/processed");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "data/processed/crypto_processed_" + timestamp + ".json";

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            for (CryptoPrice obj : data) {
                String jsonLine = objectMapper.writeValueAsString(obj);
                writer.write(jsonLine);
                writer.newLine();
            }

            writer.close();

            System.out.println("PROCESSED data saved at: " + fileName);

        } catch (Exception e) {
            System.out.println("Error saving processed data: " + e.getMessage());
        }
    }
}
