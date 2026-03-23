package com.crypto_data_platform.crypto_data_platform.scheduler;

import com.crypto_data_platform.crypto_data_platform.service.CryptoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// AUTOMATIZAR EL DATA PIPELINE
@Component
public class CryptoScheduler {

    private final CryptoService service;

    public CryptoScheduler(CryptoService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 60000) // cada 60 segundos
    public void runCryptoPipeline() {

        System.out.println("Starting scheduled crypto ingestion...");
        try {
            service.fetchAndSaveCryptoData();
        } catch (Exception e) {
            System.out.println("Scheduler ERROR: " + e.getMessage());
        }
        System.out.println("Finished scheduled crypto ingestion");
    }
}
