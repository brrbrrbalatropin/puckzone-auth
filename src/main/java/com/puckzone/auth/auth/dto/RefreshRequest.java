package com.puckzone.auth.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/auth/refresh: el refresh token vigente. La respuesta
 * trae un access y un refresh NUEVOS (expiracion deslizante: mientras el
 * jugador siga activo cada semana, nunca vuelve a ver el login).
 */
public record RefreshRequest(
        @NotBlank(message = "el refreshToken es obligatorio") String refreshToken
) {
}
