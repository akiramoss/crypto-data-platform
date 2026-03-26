package com.crypto_data_platform.crypto_data_platform.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private Double price;

    private Double marketCap;

    private Double volume;

    // Guardar tiempo real legible
    // HIBERNATE --> DATETIME
    private LocalDateTime timeStamp;
}