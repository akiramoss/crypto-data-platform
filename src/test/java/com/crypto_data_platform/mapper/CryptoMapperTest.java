package com.crypto_data_platform.mapper;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoMapperTest {

    private static CryptoApiResponse validDto() {
        CryptoApiResponse dto = new CryptoApiResponse();
        dto.setId("bitcoin");
        dto.setSymbol("btc");
        dto.setName("Bitcoin");
        dto.setCurrent_price(65000.5);
        dto.setMarket_cap(1_200_000_000.0);
        dto.setTotal_volume(50_000_000.0);
        dto.setLast_updated("2024-01-15T10:30:00.000Z");
        return dto;
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly_whenValidResponse() {
        // Arrange
        CryptoApiResponse dto = validDto();

        // Act
        CryptoPrice entity = CryptoMapper.toEntity(dto);

        // Assert
        assertThat(entity.getSymbol()).isEqualTo("btc");
        assertThat(entity.getPrice()).isEqualTo(65000.5);
        assertThat(entity.getMarketCap()).isEqualTo(1_200_000_000.0);
        assertThat(entity.getVolume()).isEqualTo(50_000_000.0);
        assertThat(entity.getEventTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void toEntity_setsIngestionTimestampCloseToNow() {
        // Arrange
        CryptoApiResponse dto = validDto();
        LocalDateTime before = LocalDateTime.now();

        // Act
        CryptoPrice entity = CryptoMapper.toEntity(dto);

        // Assert
        LocalDateTime after = LocalDateTime.now();
        assertThat(entity.getTimeStamp()).isNotNull();
        assertThat(entity.getTimeStamp()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void toEntity_keepsNullNumericFields_whenDtoFieldsAreNull() {
        // Arrange: last_updated must stay valid, but price/marketCap/volume are null
        CryptoApiResponse dto = validDto();
        dto.setCurrent_price(null);
        dto.setMarket_cap(null);
        dto.setTotal_volume(null);

        // Act
        CryptoPrice entity = CryptoMapper.toEntity(dto);

        // Assert
        assertThat(entity.getPrice()).isNull();
        assertThat(entity.getMarketCap()).isNull();
        assertThat(entity.getVolume()).isNull();
        // eventTime parsing still works fine since last_updated is valid
        assertThat(entity.getEventTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void toEntity_throwsNullPointerException_whenLastUpdatedIsNull() {
        // BUG (documented, not fixed): CryptoMapper.toEntity (mapper/CryptoMapper.java:21-22) calls
        // OffsetDateTime.parse(dto.getLast_updated()) without a null-check. If the API ever returns
        // a record without "last_updated", the whole mapping call blows up with an unchecked NPE
        // instead of a meaningful validation error. Since CryptoService#mapToEntities has no
        // try/catch per item, ONE such record aborts the mapping of the entire batch (see
        // CryptoServiceTest for the batch-level consequence).
        CryptoApiResponse dto = validDto();
        dto.setLast_updated(null);

        assertThatThrownBy(() -> CryptoMapper.toEntity(dto))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toEntity_throwsDateTimeParseException_whenLastUpdatedHasInvalidFormat() {
        // Documents current behavior: an unparsable last_updated value propagates a
        // DateTimeParseException out of the mapper (not swallowed / not defaulted).
        CryptoApiResponse dto = validDto();
        dto.setLast_updated("not-a-valid-date");

        assertThatThrownBy(() -> CryptoMapper.toEntity(dto))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void toEntity_throwsDateTimeParseException_whenLastUpdatedHasNoOffset() {
        // OffsetDateTime.parse requires an explicit offset/zone (e.g. "Z" or "+02:00").
        // A plain local date-time string (no offset) is rejected, which is a plausible
        // real-world input if an upstream API changes its date format.
        CryptoApiResponse dto = validDto();
        dto.setLast_updated("2024-01-15T10:30:00");

        assertThatThrownBy(() -> CryptoMapper.toEntity(dto))
                .isInstanceOf(DateTimeParseException.class);
    }
}
