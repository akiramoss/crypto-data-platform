package com.crypto_data_platform.mapper;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CryptoMapper {

    public CryptoPrice toEntity(CryptoApiResponse dto) {

        if (dto.getLastUpdated() == null) {
            throw new IllegalArgumentException("last_updated is required to map a CryptoApiResponse but was null (symbol=" + dto.getSymbol() + ")");
        }

        CryptoPrice entity = new CryptoPrice();

        entity.setSymbol(dto.getSymbol());
        entity.setPrice(dto.getCurrentPrice());
        entity.setMarketCap(dto.getMarketCap());
        entity.setVolume(dto.getTotalVolume());

        // Event time real desde la API, normalizado a UTC (eventTime y timestamp deben
        // quedar en la misma zona para poder compararse)
        LocalDateTime eventTime = OffsetDateTime.parse(dto.getLastUpdated())
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        // Ingestion Time (cuando guardamos los datos), también en UTC
        LocalDateTime ingestionTime = LocalDateTime.now(ZoneOffset.UTC);

        entity.setEventTime(eventTime);
        entity.setTimestamp(ingestionTime);

        return entity;
    }
}
