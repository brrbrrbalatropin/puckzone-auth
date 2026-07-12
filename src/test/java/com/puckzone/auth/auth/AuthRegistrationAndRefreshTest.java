package com.puckzone.auth.auth;

import com.puckzone.auth.auth.dto.AuthResponse;
import com.puckzone.auth.auth.dto.RefreshRequest;
import com.puckzone.auth.auth.dto.RegisterRequest;
import com.puckzone.auth.auth.exception.EmailAlreadyUsedException;
import com.puckzone.auth.auth.exception.InvalidEmailDomainException;
import com.puckzone.auth.auth.exception.InvalidRefreshTokenException;
import com.puckzone.auth.auth.exception.UsernameAlreadyUsedException;
import com.puckzone.auth.user.User;
import com.puckzone.auth.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas del registro (solo correos .edu.co colombianos, universidad =
 * etiqueta justo antes de .edu.co, sin duplicados, email normalizado) y
 * del refresh (rota el par solo con un refresh token vigente de un usuario
 * que siga existiendo). Complementa a AuthServiceTest (bloqueo del login).
 */
@ExtendWith(MockitoExtension.class)
class AuthRegistrationAndRefreshTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    private static RegisterRequest register(String email) {
        return new RegisterRequest("daniel", email, "secreta123");
    }

    private void stubHappyPath() {
        when(userService.emailExists(any())).thenReturn(false);
        when(userService.usernameExists(any())).thenReturn(false);
        when(userService.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("secreta123")).thenReturn("$hash$");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-jwt");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-jwt");
    }

    @Test
    void elRegistroGuardaHashYUniversidadYEmiteElPar() {
        stubHappyPath();

        AuthResponse response = authService.register(register("daniel@eci.edu.co"));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("daniel@eci.edu.co", saved.getEmail());
        assertEquals("eci", saved.getUniversity());
        assertEquals("$hash$", saved.getPasswordHash(), "nunca se guarda la contraseña en claro");
        assertEquals("access-jwt", response.token());
        assertEquals("refresh-jwt", response.refreshToken());
        assertEquals("eci", response.university());
    }

    @Test
    void laUniversidadEsLaEtiquetaJustoAntesDeEduCo() {
        stubHappyPath();

        // Subdominios de correo: "correo.unal.edu.co" es la universidad "unal".
        authService.register(register("juan@correo.unal.edu.co"));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        assertEquals("unal", captor.getValue().getUniversity());
    }

    @Test
    void elEmailSeNormalizaAntesDeGuardar() {
        stubHappyPath();

        authService.register(register("  DANIEL@ECI.EDU.CO  "));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        assertEquals("daniel@eci.edu.co", captor.getValue().getEmail());
    }

    @Test
    void losCorreosNoInstitucionalesSeRechazan() {
        assertThrows(InvalidEmailDomainException.class,
                () -> authService.register(register("daniel@gmail.com")));
        assertThrows(InvalidEmailDomainException.class,
                () -> authService.register(register("daniel@mit.edu")));
        // "edu.co" pegado sin ser el sufijo institucional real
        assertThrows(InvalidEmailDomainException.class,
                () -> authService.register(register("daniel@fakeedu.co")));
        verify(userService, never()).save(any());
    }

    @Test
    void elEmailYElUsernameDuplicadosSeRechazan() {
        when(userService.emailExists("repetido@eci.edu.co")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class,
                () -> authService.register(register("repetido@eci.edu.co")));

        when(userService.emailExists("daniel@eci.edu.co")).thenReturn(false);
        when(userService.usernameExists("daniel")).thenReturn(true);
        assertThrows(UsernameAlreadyUsedException.class,
                () -> authService.register(register("daniel@eci.edu.co")));
        verify(userService, never()).save(any());
    }

    @Test
    void elRefreshVigenteRotaElParConDatosFrescosDeLaBd() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("daniel");
        user.setUniversity("eci");
        when(jwtService.parseRefreshTokenSubject("refresh-viejo")).thenReturn(userId.toString());
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-nuevo");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-nuevo");

        AuthResponse response = authService.refresh(new RefreshRequest("refresh-viejo"));

        assertEquals("access-nuevo", response.token());
        assertEquals("refresh-nuevo", response.refreshToken());
        assertEquals("daniel", response.username());
    }

    @Test
    void unRefreshInvalidoODeUsuarioBorradoSeRechaza() {
        // El token no es un refresh válido (firma mala, vencido o type!=refresh).
        when(jwtService.parseRefreshTokenSubject("chatarra"))
                .thenThrow(new InvalidRefreshTokenException());
        assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(new RefreshRequest("chatarra")));

        // El subject no es un UUID.
        when(jwtService.parseRefreshTokenSubject("sub-raro")).thenReturn("no-soy-uuid");
        assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(new RefreshRequest("sub-raro")));

        // El usuario del token ya no existe en la BD.
        UUID ghost = UUID.randomUUID();
        when(jwtService.parseRefreshTokenSubject("de-fantasma")).thenReturn(ghost.toString());
        when(userService.findById(ghost)).thenReturn(Optional.empty());
        assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(new RefreshRequest("de-fantasma")));

        verify(jwtService, never()).generateAccessToken(any());
    }
}
