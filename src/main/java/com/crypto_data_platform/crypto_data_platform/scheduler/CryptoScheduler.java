package com.crypto_data_platform.crypto_data_platform.scheduler;

import com.crypto_data_platform.crypto_data_platform.service.CryptoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// AUTOMATIZAR EL DATA PIPELINE
@Component
public class CryptoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CryptoScheduler.class);

    private final CryptoService service;

    public CryptoScheduler(CryptoService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 60000) // cada 60 segundos
    public void runCryptoPipeline() {

        logger.info("Starting scheduled crypto ingestion...");
        try {
            service.fetchAndSaveCryptoData();
        } catch (Exception e) {
            logger.error("Scheduler ERROR:", e);
        }
        logger.info("Finished scheduled crypto ingestion");
    }
}
