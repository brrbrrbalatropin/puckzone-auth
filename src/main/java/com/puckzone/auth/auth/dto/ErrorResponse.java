package com.puckzone.auth.auth.dto;

/**
 * Cuerpo JSON uniforme para respuestas de error.
 */
public record ErrorResponse(
        int status,
        String error,
        String message
) {
}