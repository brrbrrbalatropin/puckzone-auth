package com.puckzone.auth.auth.exception;

/**
 * La cuenta esta bloqueada temporalmente por intentos fallidos de login
 * (proteccion contra fuerza bruta). Mientras dure el bloqueo se rechaza
 * incluso la contrasena correcta.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(long minutesLeft) {
        super("Cuenta bloqueada por intentos fallidos. Intenta de nuevo en "
                + minutesLeft + (minutesLeft == 1 ? " minuto." : " minutos."));
    }
}
