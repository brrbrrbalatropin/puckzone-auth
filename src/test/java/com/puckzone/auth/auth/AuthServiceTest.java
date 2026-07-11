package com.puckzone.auth.auth;

import com.puckzone.auth.auth.dto.LoginRequest;
import com.puckzone.auth.auth.exception.AccountLockedException;
import com.puckzone.auth.auth.exception.InvalidCredentialsException;
import com.puckzone.auth.user.User;
import com.puckzone.auth.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bloqueo por fuerza bruta en el login: 5 contrasenas malas bloquean la
 * cuenta 15 minutos (incluso para la contrasena correcta); entrar bien
 * resetea el contador. Sin BD: UserService va mockeado y se verifica que
 * los contadores SE GUARDEN (persisten aunque el login lance excepcion).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final LoginRequest LOGIN = new LoginRequest("dev@eci.edu.co", "secreta123");

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("dev@eci.edu.co");
        user.setUsername("dev");
        user.setUniversity("eci");
        user.setPasswordHash("$hash$");
        when(userService.findByEmail("dev@eci.edu.co")).thenReturn(Optional.of(user));
    }

    @Test
    void unFalloIncrementaElContadorYLoGuarda() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(LOGIN));

        assertEquals(1, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
        verify(userService).save(user);
    }

    @Test
    void elQuintoFalloBloqueaLaCuentaYReseteaElContador() {
        user.setFailedLoginAttempts(4);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(AccountLockedException.class, () -> authService.login(LOGIN));

        assertNotNull(user.getLockedUntil(), "el quinto fallo no dejo candado");
        assertTrue(user.getLockedUntil().isAfter(Instant.now()), "el candado no apunta al futuro");
        assertEquals(0, user.getFailedLoginAttempts(),
                "al expirar el candado debe arrancar con los intentos completos");
        verify(userService).save(user);
    }

    @Test
    void bloqueadaRechazaInclusoLaContrasenaCorrectaSinVerificarla() {
        user.setLockedUntil(Instant.now().plusSeconds(600));

        assertThrows(AccountLockedException.class, () -> authService.login(LOGIN));

        // Ni siquiera se compara la contrasena: bloqueada es bloqueada.
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void bloqueoExpiradoDejaEntrarYLimpiaElCandado() {
        user.setLockedUntil(Instant.now().minusSeconds(1));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertNotNull(authService.login(LOGIN));

        assertNull(user.getLockedUntil());
        assertEquals(0, user.getFailedLoginAttempts());
        verify(userService).save(user);
    }

    @Test
    void loginExitosoReseteaLosFallosAcumulados() {
        user.setFailedLoginAttempts(3);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertNotNull(authService.login(LOGIN));

        assertEquals(0, user.getFailedLoginAttempts());
        verify(userService).save(user);
    }

    @Test
    void loginExitosoSinFallosPreviosNoTocaLaBase() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertNotNull(authService.login(LOGIN));

        verify(userService, never()).save(any());
    }
}
