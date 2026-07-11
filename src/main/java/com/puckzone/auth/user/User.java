package com.puckzone.auth.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String university;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    /**
     * Fallos de contraseña consecutivos; se resetea al entrar o al bloquear.
     * El DEFAULT es imprescindible: sin él, el ALTER de ddl-auto update
     * falla en la tabla ya poblada de producción (NOT NULL sin relleno)
     * y la columna nunca se crearía.
     */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int failedLoginAttempts;

    /**
     * Hasta cuándo está bloqueado el login por intentos fallidos (null =
     * sin bloqueo). En BD y no en memoria: el candado sobrevive reinicios
     * y aplica igual si auth corre con varias réplicas.
     */
    @Column
    private Instant lockedUntil;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}