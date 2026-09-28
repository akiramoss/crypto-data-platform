package com.crypto_data_platform.scheduler;

import com.crypto_data_platform.service.CryptoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CryptoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CryptoScheduler.class);

    private final CryptoService service;

    public CryptoScheduler(CryptoService service) {
        this.service = service;
    }

    @Scheduled(fixedRateString = "${crypto.scheduler.fixed-rate-ms:300000}")
    public void runCryptoPipeline() {

        logger.info("Starting scheduled crypto ingestion...");
        try {
            service.fetchAndSaveCryptoData();
        } catch (Exception e) {
            // Última red de seguridad: CryptoService ya maneja los fallos esperados
            // (API, BD); esto solo debe activarse ante un error de programación
            // inesperado, y nunca debe tumbar el hilo del scheduler.
            logger.error("Scheduler ERROR:", e);
        }
        logger.info("Finished scheduled crypto ingestion");
    }
}
