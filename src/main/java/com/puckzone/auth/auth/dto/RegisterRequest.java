package com.puckzone.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para el registro. La regla del dominio .edu.co se
 * valida en AuthService (no se puede expresar bien solo con @Email).
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}