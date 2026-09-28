package com.crypto_data_platform.service;

import com.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.mapper.CryptoMapper;
import com.crypto_data_platform.repository.CryptoRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);

    private final CryptoApiClient apiClient;
    private final CryptoRepository repository;
    private final RawDataService rawDataService;
    private final ProcessedDataService processedDataService;
    private final CryptoMapper mapper;
    private final PriceFluctuationService fluctuationService;

    public CryptoService(CryptoApiClient apiClient, CryptoRepository repository, RawDataService rawDataService, ProcessedDataService processedDataService, CryptoMapper mapper, PriceFluctuationService fluctuationService) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.rawDataService = rawDataService;
        this.processedDataService = processedDataService;
        this.mapper = mapper;
        this.fluctuationService = fluctuationService;
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
            fluctuationService.applyFluctuations(entities);
            persistEntitiesAndProcessedCopy(entities);

            logger.info("Data ingestion completed");

        } catch (RestClientException e) {
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
                entities.add(mapper.toEntity(dto));
            } catch (Exception e) {
                logger.warn("Skipping malformed record symbol={}: {}", dto.getSymbol(), e.getMessage());
            }
        }

        logger.info("Saving {} new entities", entities.size());
        return entities;
    }

    private void persistEntitiesAndProcessedCopy(List<CryptoPrice> entities) {
        // Deduplicamos por (symbol, eventTime) DENTRO del propio lote antes de comprobar contra BD:
        // si la API devolviera dos registros con la misma clave en la misma respuesta, ambos
        // pasarían el pre-check de "ya existe en BD" (todavía no existe ninguno) y llegarían juntos
        // a saveAll(...), donde la restricción única los rechazaría a los DOS Y a cualquier otro
        // symbol del mismo lote, perdiendo la persistencia de todo el ciclo en vez de solo del
        // duplicado.
        List<CryptoPrice> deduplicated = deduplicateBySymbolAndEventTime(entities);

        List<CryptoPrice> newEntities = deduplicated.stream()
                .filter(entity -> !repository.existsBySymbolAndEventTime(entity.getSymbol(), entity.getEventTime()))
                .toList();

        int skipped = entities.size() - newEntities.size();
        if (skipped > 0) {
            logger.warn("Skipped {} duplicate record(s) (already in the database or duplicated within this batch)",
                    skipped);
        }

        try {
            repository.saveAll(newEntities);
        } catch (DataAccessException e) {
            logger.error("Unexpected error while persisting crypto entities", e);
        }

        // La copia PROCESSED refleja siempre lo que se ha procesado en este ciclo,
        // independientemente de si algún registro ya existía en BD.
        processedDataService.saveProcessedData(entities);
    }

    private List<CryptoPrice> deduplicateBySymbolAndEventTime(List<CryptoPrice> entities) {
        Map<String, CryptoPrice> uniqueByKey = new LinkedHashMap<>();
        for (CryptoPrice entity : entities) {
            uniqueByKey.putIfAbsent(entity.getSymbol() + "|" + entity.getEventTime(), entity);
        }
        return new ArrayList<>(uniqueByKey.values());
    }
}
