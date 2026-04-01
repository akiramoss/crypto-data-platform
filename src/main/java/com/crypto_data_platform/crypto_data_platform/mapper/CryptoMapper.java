package com.crypto_data_platform.crypto_data_platform.mapper;

import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class CryptoMapper {

    public static CryptoPrice toEntity(CryptoApiResponse dto) {

        CryptoPrice entity = new CryptoPrice();

        entity.setSymbol(dto.getSymbol());
        entity.setPrice(dto.getCurrent_price());
        entity.setMarketCap(dto.getMarket_cap());
        entity.setVolume(dto.getTotal_volume());

        // Event time real desde la API
        LocalDateTime eventTime = OffsetDateTime.
                parse(dto.getLast_updated()).toLocalDateTime();

        // Ingestion Time (cuando guardamos los datos)
        LocalDateTime ingestionTime = LocalDateTime.now();

        entity.setEventTime(eventTime);
        entity.setTimeStamp(ingestionTime);

        return entity;
    }
}
