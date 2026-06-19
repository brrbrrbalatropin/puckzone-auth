package com.puckzone.auth.auth.exception;

/** El correo no pertenece a un dominio institucional colombiano (.edu.co). */
public class InvalidEmailDomainException extends RuntimeException {
    public InvalidEmailDomainException(String email) {
        super("El correo '" + email + "' no es un correo institucional .edu.co valido.");
    }
}