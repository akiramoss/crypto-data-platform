package com.crypto_data_platform.dto;

import java.time.Instant;

/**
 * Cuerpo de error consistente devuelto por GlobalExceptionHandler para cualquier error de
 * validación o de request mal formado en la API REST.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
