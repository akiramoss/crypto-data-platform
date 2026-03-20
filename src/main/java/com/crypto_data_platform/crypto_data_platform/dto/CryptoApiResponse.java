package com.crypto_data_platform.crypto_data_platform.dto;

import lombok.Data;

@Data
public class CryptoApiResponse {

    private String id;

    private String symbol;

    private String name;

    private Double current_price;

    private Double market_cap;

    private Double total_value;
}
