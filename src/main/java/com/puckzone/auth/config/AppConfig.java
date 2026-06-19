package com.puckzone.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans de infraestructura del servicio de autenticacion.
 * <p>
 * Como solo usamos spring-security-crypto (sin el autoconfig de Spring
 * Security), el bean PasswordEncoder debe declararse manualmente.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}