package com.crypto_data_platform.service;

import com.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.mapper.CryptoMapper;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);

    private final CryptoApiClient apiClient;
    private final CryptoRepository repository;
    private final RawDataService rawDataService;
    private final ProcessedDataService processedDataService;

    public CryptoService(CryptoApiClient apiClient, CryptoRepository repository, RawDataService rawDataService, ProcessedDataService processedDataService) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.rawDataService = rawDataService;
        this.processedDataService = processedDataService;
    }

    /**
     * 1. Llama a la API
     * 2. Guarda datos RAW
     * 3. Transforma DTO → Entity
     * 4. Inserta en DB (control de duplicados en DB)
     * 5. Guarda datos PROCESSED
     */
    public void fetchAndSaveCryptoData() {
        try {
            CryptoApiResponse[] response = fetchFromApiAndSaveRaw();
            List<CryptoPrice> entities = mapToEntities(response);
            persistEntitiesAndProcessedCopy(entities);

            logger.info("Data ingestion completed");

        } catch (Exception e) {
            logger.error("ERROR during crypto ingestion", e);
        }
    }

    private CryptoApiResponse[] fetchFromApiAndSaveRaw() {
        CryptoApiResponse[] response = apiClient.fetchCryptoData();

        if (response == null) {
            logger.warn("API returned a null response, skipping this ingestion cycle");
            return new CryptoApiResponse[0];
        }

        // Guardamos datos RAW antes de procesarlos
        rawDataService.saveRawData(response);

        logger.info("Fetched {} records from API", response.length);
        return response;
    }

    private List<CryptoPrice> mapToEntities(CryptoApiResponse[] response) {
        List<CryptoPrice> entities = new ArrayList<>();

        for (CryptoApiResponse dto : response) {
            try {
                entities.add(CryptoMapper.toEntity(dto));
            } catch (Exception e) {
                logger.warn("Skipping malformed record symbol={}: {}", dto.getSymbol(), e.getMessage());
            }
        }

        logger.info("Saving {} new entities", entities.size());
        return entities;
    }

    private void persistEntitiesAndProcessedCopy(List<CryptoPrice> entities) {
        List<CryptoPrice> newEntities = entities.stream()
                .filter(entity -> !repository.existsBySymbolAndEventTime(entity.getSymbol(), entity.getEventTime()))
                .toList();

        if (newEntities.size() < entities.size()) {
            logger.warn("Skipped {} duplicate record(s) already present in the database",
                    entities.size() - newEntities.size());
        }

        try {
            repository.saveAll(newEntities);
        } catch (Exception e) {
            logger.error("Unexpected error while persisting crypto entities", e);
        }

        // La copia PROCESSED refleja siempre lo que se ha procesado en este ciclo,
        // independientemente de si algún registro ya existía en BD.
        processedDataService.saveProcessedData(entities);
    }
}
