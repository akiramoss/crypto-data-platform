package com.crypto_data_platform.crypto_data_platform.mapper;

import com.crypto_data_platform.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.crypto_data_platform.dto.CryptoApiResponse;

import java.time.LocalDateTime;

public class CryptoMapper {

    public static CryptoPrice toEntity(CryptoApiResponse dto) {

        CryptoPrice entity = new CryptoPrice();

        entity.setSymbol(dto.getSymbol());
        entity.setPrice(dto.getCurrent_price());
        entity.setMarketCap(dto.getMarket_cap());
        entity.setVolume(dto.getTotal_volume());

        LocalDateTime now = LocalDateTime.now();

        entity.setEventTime(now);
        entity.setTimeStamp(now);

        return entity;
    }
}