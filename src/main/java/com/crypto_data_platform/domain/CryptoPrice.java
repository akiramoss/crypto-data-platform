package com.crypto_data_platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Se usan {@code @Getter}/{@code @Setter} en lugar de {@code @Data}: una entidad JPA no debe
 * generar {@code equals}/{@code hashCode}/{@code toString} automáticos (pueden disparar la
 * carga de proxies/colecciones lazy o romper el contrato de igualdad basado en el id).
 */
@Getter
@Setter
@Entity
@Table(name = "crypto_price",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "event_time"}))
public class CryptoPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private Double price;
    private Double marketCap;
    private Double volume;

    private LocalDateTime eventTime;
    private LocalDateTime timestamp;

    /**
     * Variación porcentual de {@code price} respecto al registro anterior almacenado para el
     * mismo symbol (calculada por {@link com.crypto_data_platform.service.PriceFluctuationService}).
     * {@code null} si este es el primer registro conocido de ese symbol.
     */
    private Double priceFluctuation;
}
