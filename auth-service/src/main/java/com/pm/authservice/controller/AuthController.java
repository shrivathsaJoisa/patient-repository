package com.pm.authservice.controller;

import com.pm.authservice.dto.CreateUserRequestDTO;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.dto.UserResponseDTO;
import com.pm.authservice.model.User;
import com.pm.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
//Auth Controller

@RestController
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {

        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);

        if(tokenOptional.isEmpty()){
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = tokenOptional.get();
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @Operation(summary = "Validating the JWT token")
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader){
        //Authorization: Bearer <token>
        if(authHeader == null || !authHeader.startsWith("Bearer ")) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return authService.validateToken(authHeader.substring(7))
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    }

    @Operation(summary = "Create a new user (ADMIN only)")
    @PostMapping("/admin/users")
    public ResponseEntity<UserResponseDTO> createUser(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateUserRequestDTO createUserRequestDTO) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!authService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<String> roleOptional = authService.extractRole(token);
        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"ADMIN".equalsIgnoreCase(roleOptional.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<User> createdUserOptional = authService.createUser(createUserRequestDTO);
        if (createdUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User createdUser = createdUserOptional.get();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponseDTO(createdUser.getId(), createdUser.getEmail(), createdUser.getRole()));
    }
}
