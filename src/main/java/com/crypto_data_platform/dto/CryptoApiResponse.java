package com.crypto_data_platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CryptoApiResponse {

    private String id;
    private String symbol;
    private String name;

    @JsonProperty("current_price")
    private BigDecimal currentPrice;

    @JsonProperty("market_cap")
    private BigDecimal marketCap;

    @JsonProperty("total_volume")
    private BigDecimal totalVolume;

    @JsonProperty("last_updated")
    private String lastUpdated;
}
