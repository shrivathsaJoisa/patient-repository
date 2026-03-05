package com.pm.authservice.service;

import com.pm.authservice.dto.CreateUserRequestDTO;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.model.User;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil){
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO){
        try {
            return userService
                    .findByEmail(loginRequestDTO.getEmail())
                    .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))
                    .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole()));
        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid password hash configured for user {}", loginRequestDTO.getEmail());
            return Optional.empty();
        }
    }

    public boolean validateToken(String token){
        try{
            jwtUtil.validateToken(token);return true;}
            catch (JwtException e) {return false;}

    }

    public Optional<String> extractRole(String token) {
        try {
            return Optional.ofNullable(jwtUtil.extractRole(token));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public Optional<User> createUser(CreateUserRequestDTO createUserRequestDTO) {
        if (userService.findByEmail(createUserRequestDTO.getEmail()).isPresent()) {
            return Optional.empty();
        }

        User user = new User();
        user.setEmail(createUserRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequestDTO.getPassword()));
        user.setRole(createUserRequestDTO.getRole().toUpperCase());

        return Optional.of(userService.save(user));
    }
}
