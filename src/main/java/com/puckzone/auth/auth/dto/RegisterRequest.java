package com.puckzone.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para el registro. La regla del dominio .edu.co se
 * valida en AuthService (no se puede expresar bien solo con @Email).
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        // El username sale publicado en los leaderboards, así que no puede ser
        // un correo: alguien se registró con el suyo y quedó su dirección
        // institucional (truncada a 30) a la vista de todos. Solo se prohíbe la
        // arroba — los nombres reales llevan espacios y puntos ("Juanes Cruz").
        @Pattern(regexp = "[^@]+", message = "El nombre de usuario no puede ser un correo")
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}