package com.puckzone.auth.auth.exception;

/** Ya existe un usuario registrado con ese correo. */
public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("El correo '" + email + "' ya esta registrado.");
    }
}