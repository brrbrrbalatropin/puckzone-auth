package com.puckzone.auth.auth;

import com.puckzone.auth.auth.dto.AuthResponse;
import com.puckzone.auth.auth.dto.LoginRequest;
import com.puckzone.auth.auth.dto.RefreshRequest;
import com.puckzone.auth.auth.dto.RegisterRequest;
import com.puckzone.auth.auth.exception.EmailAlreadyUsedException;
import com.puckzone.auth.auth.exception.InvalidCredentialsException;
import com.puckzone.auth.auth.exception.InvalidEmailDomainException;
import com.puckzone.auth.auth.exception.InvalidRefreshTokenException;
import com.puckzone.auth.auth.exception.UsernameAlreadyUsedException;
import com.puckzone.auth.user.User;
import com.puckzone.auth.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquesta el registro y el login. Contiene las reglas de negocio de
 * autenticacion; delega la persistencia en UserService, el hashing en
 * PasswordEncoder y la emision de tokens en JwtService.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Correo institucional colombiano: algo@<dominio>.edu.co
     * El grupo 1 captura todo el dominio antes de ".edu.co"
     * (p.ej. "eci" o "correo.unal").
     */
    private static final Pattern INSTITUTIONAL_EMAIL =
            Pattern.compile("^[^@\\s]+@([^@\\s]+)\\.edu\\.co$", Pattern.CASE_INSENSITIVE);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String university = extractUniversity(email);

        if (userService.emailExists(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        if (userService.usernameExists(request.username())) {
            throw new UsernameAlreadyUsedException(request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUniversity(university);

        User saved = userService.save(user);
        return issueTokens(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userService.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    /**
     * Cambia un refresh token vigente por un par nuevo de tokens. El usuario
     * se recarga de la BD: si fue borrado el refresh deja de servir, y los
     * claims del access salen frescos (no arrastran datos viejos del token).
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String subject = jwtService.parseRefreshTokenSubject(request.refreshToken());
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidRefreshTokenException();
        }
        User user = userService.findById(userId)
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(user);
    }

    /** Par access (1h) + refresh (7d) para register, login y refresh. */
    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getUsername(),
                user.getUniversity());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Valida que el correo sea institucional (.edu.co) y extrae la
     * universidad: la etiqueta inmediatamente antes de ".edu.co".
     * Ej: "juan@correo.unal.edu.co" -> "unal"; "ana@eci.edu.co" -> "eci".
     */
    private String extractUniversity(String email) {
        Matcher matcher = INSTITUTIONAL_EMAIL.matcher(email);
        if (!matcher.matches()) {
            throw new InvalidEmailDomainException(email);
        }
        String domain = matcher.group(1);
        int lastDot = domain.lastIndexOf('.');
        return lastDot >= 0 ? domain.substring(lastDot + 1) : domain;
    }
}