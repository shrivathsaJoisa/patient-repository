package com.pm.authservice.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key secreteKey;

    public JwtUtil(@Value("${jwt.secrete}") String secrete){
        byte[] keyBytes = Base64.getDecoder().decode(secrete.getBytes(StandardCharsets.UTF_8));
        this.secreteKey = Keys.hmacShaKeyFor(keyBytes);

    }

    public String generateToken(String email, String role){
        return Jwts.builder()
                .subject(email)
                .claim("role",role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(secreteKey)
                .compact();
    }

    public void validateToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) secreteKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (SignatureException e){
            throw new JwtException("Invalid Signature");
        } catch (JwtException e) {
            throw new JwtException("Invalid JWT token");}
    }

    public String extractRole(String token) {
        return Jwts.parser().verifyWith((SecretKey) secreteKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}
