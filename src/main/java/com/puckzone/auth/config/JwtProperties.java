package com.puckzone.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion tipada del JWT, mapeada desde la seccion "puckzone.jwt"
 * de application.yaml.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "puckzone.jwt")
public class JwtProperties {

    /** Clave secreta para firmar los tokens (HS256, minimo 32 bytes). */
    private String secret;

    /** Vida del access token en milisegundos (corto: viaja en cada request). */
    private long accessExpirationMs;

    /** Vida del refresh token en milisegundos (largo: solo se usa en /refresh). */
    private long refreshExpirationMs;
}
