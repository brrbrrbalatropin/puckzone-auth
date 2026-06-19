package com.puckzone.auth.auth.dto;

/**
 * Respuesta de registro y login: el token JWT y datos basicos de perfil
 * para que el frontend pinte la sesion sin decodificar el token.
 */
public record AuthResponse(
        String token,
        String username,
        String university
) {
}