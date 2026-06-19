package com.puckzone.auth.auth;

import com.puckzone.auth.config.JwtProperties;
import com.puckzone.auth.user.User;
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
 * El token incluye el id del usuario (subject) y datos de perfil (email,
 * username, university) para que el resto de microservicios puedan
 * identificar al jugador sin volver a consultar a puckzone-auth.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.getExpirationMs();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("university", user.getUniversity())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }
}