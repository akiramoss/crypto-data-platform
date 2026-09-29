package com.crypto_data_platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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

    // precision/scale explícitos: dejar que Hibernate infiera un DOUBLE/FLOAT a partir de
    // BigDecimal reintroduciría el mismo redondeo binario que este tipo pretende evitar.
    @Column(precision = 24, scale = 8)
    private BigDecimal price;

    @Column(precision = 24, scale = 8)
    private BigDecimal marketCap;

    @Column(precision = 24, scale = 8)
    private BigDecimal volume;

    private LocalDateTime eventTime;
    private LocalDateTime timestamp;

    /**
     * Variación porcentual de {@code price} respecto al registro anterior almacenado para el
     * mismo symbol (calculada por {@link com.crypto_data_platform.service.PriceFluctuationService}).
     * {@code null} si este es el primer registro conocido de ese symbol.
     */
    @Column(precision = 12, scale = 4)
    private BigDecimal priceFluctuation;
}
