package com.puckzone.auth.auth.exception;

/** Credenciales invalidas en el login (correo inexistente o contrasena incorrecta). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Correo o contrasena incorrectos.");
    }
}