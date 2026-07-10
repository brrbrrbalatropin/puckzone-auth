package com.puckzone.auth.auth.dto;

/**
 * Respuesta de registro, login y refresh: el access token (corto, va en
 * cada request), el refresh token (largo, solo sirve en /refresh) y datos
 * basicos de perfil para que el frontend pinte la sesion sin decodificar
 * el token.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String username,
        String university
) {
}
