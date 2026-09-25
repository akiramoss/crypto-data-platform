package com.crypto_data_platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CryptoApiResponse {

    private String id;
    private String symbol;
    private String name;

    @JsonProperty("current_price")
    private Double currentPrice;

    @JsonProperty("market_cap")
    private Double marketCap;

    @JsonProperty("total_volume")
    private Double totalVolume;

    @JsonProperty("last_updated")
    private String lastUpdated;
}
