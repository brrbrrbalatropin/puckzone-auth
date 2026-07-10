package com.puckzone.auth.auth;

import com.puckzone.auth.auth.exception.InvalidRefreshTokenException;
import com.puckzone.auth.config.JwtProperties;
import com.puckzone.auth.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y firma tokens JWT autocontenidos.
 * <p>
 * El access token (corto) incluye el id del usuario (subject) y datos de
 * perfil (email, username, university) para que el resto de microservicios
 * identifiquen al jugador sin volver a consultar a puckzone-auth.
 * <p>
 * El refresh token (largo) solo lleva el subject y el claim
 * {@code type=refresh}: no es un token de identidad para consumir en el
 * gateway, unicamente sirve para pedir un access nuevo en /refresh. El
 * gateway rechaza cualquier token con type=refresh fuera de ese endpoint.
 */
@Service
public class JwtService {

    /** Marca los refresh tokens; un access token no lleva este claim. */
    static final String TYPE_CLAIM = "type";
    static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = properties.getAccessExpirationMs();
        this.refreshExpirationMs = properties.getRefreshExpirationMs();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("university", user.getUniversity())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshExpirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida un refresh token (firma, expiracion y type=refresh) y devuelve
     * el id de usuario de su subject. Un access token aqui es invalido: sin
     * este chequeo, un access filtrado se renovaria a si mismo para siempre.
     */
    public String parseRefreshTokenSubject(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
            if (!TYPE_REFRESH.equals(claims.get(TYPE_CLAIM, String.class))) {
                throw new InvalidRefreshTokenException();
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidRefreshTokenException();
        }
    }
}
