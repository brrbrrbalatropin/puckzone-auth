package com.puckzone.auth.auth.exception;

/**
 * El refresh token no es valido: firma incorrecta, expirado, no es de tipo
 * refresh (p.ej. alguien mando un access token) o el usuario ya no existe.
 * El mensaje es deliberadamente generico para no dar pistas.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token invalido o expirado");
    }
}
