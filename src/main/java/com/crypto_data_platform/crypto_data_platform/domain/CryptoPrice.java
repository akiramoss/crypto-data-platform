package com.crypto_data_platform.crypto_data_platform.domain;

import jakarta.persistence.*;

@Entity
public class CryptoPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private Double price;
    private Double marketCap;
    private Double volume;
    private Long timestamp;
}