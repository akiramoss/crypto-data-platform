package com.crypto_data_platform.mapper;

import com.crypto_data_platform.domain.CryptoPrice;
import com.crypto_data_platform.dto.CryptoApiResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoMapperTest {

    private final CryptoMapper mapper = new CryptoMapper();

    private static CryptoApiResponse validDto() {
        CryptoApiResponse dto = new CryptoApiResponse();
        dto.setId("bitcoin");
        dto.setSymbol("btc");
        dto.setName("Bitcoin");
        dto.setCurrentPrice(BigDecimal.valueOf(65000.5));
        dto.setMarketCap(BigDecimal.valueOf(1_200_000_000.0));
        dto.setTotalVolume(BigDecimal.valueOf(50_000_000.0));
        dto.setLastUpdated("2024-01-15T10:30:00.000Z");
        return dto;
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly_whenValidResponse() {
        // Arrange
        CryptoApiResponse dto = validDto();

        // Act
        CryptoPrice entity = mapper.toEntity(dto);

        // Assert
        assertThat(entity.getSymbol()).isEqualTo("btc");
        assertThat(entity.getPrice()).isEqualByComparingTo("65000.5");
        assertThat(entity.getMarketCap()).isEqualByComparingTo("1200000000.0");
        assertThat(entity.getVolume()).isEqualByComparingTo("50000000.0");
        assertThat(entity.getEventTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void toEntity_setsIngestionTimestampCloseToNow() {
        // Arrange
        CryptoApiResponse dto = validDto();
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        // Act
        CryptoPrice entity = mapper.toEntity(dto);

        // Assert
        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);
        assertThat(entity.getTimestamp()).isNotNull();
        assertThat(entity.getTimestamp()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void toEntity_eventTimeAndTimestamp_areBothStoredInUtc_andComparable() {
        // eventTime (parsed from the API's last_updated) and timestamp (ingestion time) must
        // both live in UTC, otherwise comparing them (e.g. computing ingestion lag) is meaningless.
        // Here last_updated is set to "now" in a non-UTC offset (+05:00); if eventTime were kept in
        // that offset instead of being normalized to UTC, it would sit ~5h away from timestamp.
        OffsetDateTime nowInNonUtcOffset = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(5));
        CryptoApiResponse dto = validDto();
        dto.setLastUpdated(nowInNonUtcOffset.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        CryptoPrice entity = mapper.toEntity(dto);

        assertThat(Duration.between(entity.getEventTime(), entity.getTimestamp()).abs())
                .as("eventTime and timestamp should both be in UTC and therefore only milliseconds apart")
                .isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void toEntity_keepsNullNumericFields_whenDtoFieldsAreNull() {
        // Arrange: last_updated must stay valid, but price/marketCap/volume are null
        CryptoApiResponse dto = validDto();
        dto.setCurrentPrice(null);
        dto.setMarketCap(null);
        dto.setTotalVolume(null);

        // Act
        CryptoPrice entity = mapper.toEntity(dto);

        // Assert
        assertThat(entity.getPrice()).isNull();
        assertThat(entity.getMarketCap()).isNull();
        assertThat(entity.getVolume()).isNull();
        // eventTime parsing still works fine since last_updated is valid
        assertThat(entity.getEventTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void toEntity_throwsIllegalArgumentException_whenLastUpdatedIsNull() {
        // Fixed: CryptoMapper.toEntity now null-checks last_updated up front and throws a
        // meaningful IllegalArgumentException instead of an unchecked NPE from
        // OffsetDateTime.parse(null). CryptoService#mapToEntities still catches this per item,
        // so one such record only skips itself instead of aborting the whole batch.
        CryptoApiResponse dto = validDto();
        dto.setLastUpdated(null);

        assertThatThrownBy(() -> mapper.toEntity(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toEntity_throwsDateTimeParseException_whenLastUpdatedHasInvalidFormat() {
        // Documents current behavior: an unparsable last_updated value propagates a
        // DateTimeParseException out of the mapper (not swallowed / not defaulted).
        CryptoApiResponse dto = validDto();
        dto.setLastUpdated("not-a-valid-date");

        assertThatThrownBy(() -> mapper.toEntity(dto))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void toEntity_throwsDateTimeParseException_whenLastUpdatedHasNoOffset() {
        // OffsetDateTime.parse requires an explicit offset/zone (e.g. "Z" or "+02:00").
        // A plain local date-time string (no offset) is rejected, which is a plausible
        // real-world input if an upstream API changes its date format.
        CryptoApiResponse dto = validDto();
        dto.setLastUpdated("2024-01-15T10:30:00");

        assertThatThrownBy(() -> mapper.toEntity(dto))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void toEntity_keepsNegativePrice_withoutValidationOrRejection() {
        // Documents current behavior: CryptoMapper performs no sanity/range validation on
        // numeric fields. A negative current_price (e.g. a corrupt/erroneous upstream payload,
        // since real crypto prices are never negative) is copied through as-is, not rejected.
        CryptoApiResponse dto = validDto();
        dto.setCurrentPrice(BigDecimal.valueOf(-100.0));

        CryptoPrice entity = mapper.toEntity(dto);

        assertThat(entity.getPrice()).isEqualByComparingTo("-100.0");
    }

    @Test
    void toEntity_keepsExtremeMarketCapAndVolume_withoutOverflowOrTruncation() {
        // Arrange: an arbitrarily large value and a very small (but non-zero) positive value, to
        // check there is no silent overflow/underflow/truncation across the DTO -> entity copy.
        // Unlike double, BigDecimal has no fixed max/min magnitude, so this also documents that
        // the migration away from double actually buys arbitrary precision, not just less rounding.
        BigDecimal veryLargeMarketCap = new BigDecimal("123456789012345678901234567890.12345678");
        BigDecimal verySmallVolume = new BigDecimal("0.00000001");
        CryptoApiResponse dto = validDto();
        dto.setMarketCap(veryLargeMarketCap);
        dto.setTotalVolume(verySmallVolume);

        CryptoPrice entity = mapper.toEntity(dto);

        assertThat(entity.getMarketCap()).isEqualByComparingTo(veryLargeMarketCap);
        assertThat(entity.getVolume()).isEqualByComparingTo(verySmallVolume);
    }

    @Test
    void toEntity_keepsSymbolAsGiven_withUnicodeCharacters_noNormalizationOrValidation() {
        // CryptoMapper does not lowercase, trim or validate the symbol in any way (that
        // normalization only happens later, in CryptoController#getBySymbol via Locale.ROOT
        // lowercasing). An unusual symbol value is carried through unchanged.
        CryptoApiResponse dto = validDto();
        dto.setSymbol("₿-Ω_test 🚀");

        CryptoPrice entity = mapper.toEntity(dto);

        assertThat(entity.getSymbol()).isEqualTo("₿-Ω_test 🚀");
    }

    @Test
    void toEntity_keepsEmptySymbol_asEmptyString_withoutRejection() {
        CryptoApiResponse dto = validDto();
        dto.setSymbol("");

        CryptoPrice entity = mapper.toEntity(dto);

        assertThat(entity.getSymbol()).isEmpty();
    }

    @Test
    void toEntity_keepsNullSymbol_withoutThrowing() {
        // A null symbol only appears in the thrown message of the last_updated == null branch
        // (string concatenation just prints "null"); when last_updated IS present, a null symbol
        // does not trigger any validation at all and is copied through as null.
        CryptoApiResponse dto = validDto();
        dto.setSymbol(null);

        CryptoPrice entity = mapper.toEntity(dto);

        assertThat(entity.getSymbol()).isNull();
    }
}
