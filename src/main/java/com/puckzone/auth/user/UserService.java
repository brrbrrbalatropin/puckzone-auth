package com.puckzone.auth.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Operaciones de persistencia centradas en el usuario.
 * <p>
 * Capa delgada sobre UserRepository; la logica de autenticacion
 * (hashing, JWT, validacion de correo) vive en AuthService.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}