package com.puckzone.auth.auth.exception;

/** Ya existe un usuario con ese nombre de usuario. */
public class UsernameAlreadyUsedException extends RuntimeException {
    public UsernameAlreadyUsedException(String username) {
        super("El nombre de usuario '" + username + "' ya esta en uso.");
    }
}