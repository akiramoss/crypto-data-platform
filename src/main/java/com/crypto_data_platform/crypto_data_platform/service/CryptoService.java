package com.crypto_data_platform.crypto_data_platform.service;

import com.crypto_data_platform.crypto_data_platform.client.CryptoApiClient;
import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;
import com.crypto_data_platform.crypto_data_platform.mapper.CryptoMapper;
import com.crypto_data_platform.crypto_data_platform.repository.CryptoRepository;
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
     * 2. Recibe DTOs
     * 3. Los transforma (Mapper)
     * 4. Filtra duplicados
     * 5. Los acumula (batch)
     * 6. Los guarda en DB
     */
    public void fetchAndSaveCryptoData() {
        try {
            CryptoApiResponse[] response = apiClient.fetchCryptoData();

            // Guardamos datos RAW antes de procesarlos
            rawDataService.saveRawData(response);

            logger.info("Fetched {} records from API", response.length);

            List<CryptoPrice> entities = new ArrayList<>();

            for (CryptoApiResponse dto : response) {

                if (repository.existsBySymbol(dto.getSymbol())) {
                    continue;
                }

                CryptoPrice entity = CryptoMapper.toEntity(dto);

                entities.add(entity);
            }

            logger.info("Saving {} new entities", entities.size());

            repository.saveAll(entities);

            processedDataService.saveProcessedData(entities);

            logger.info("Data ingestion completed");

        } catch (Exception e) {
            logger.error("ERROR during crypto ingestion", e);
        }
    }
}
