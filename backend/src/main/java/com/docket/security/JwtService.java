package com.docket.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.docket.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Handles JWT generation and validation.
 * Tokens contain userId, email, and workspaceId as claims.
 * Signing: HMAC-SHA256 using the secret from application.yml → jwt.secret.
 */
@Service
public class JwtService {

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // Pad or hash the secret to ensure it meets the 256-bit minimum for HS256
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param user the authenticated user
     * @return a signed JWT string
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("workspaceId", user.getWorkspace().getId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token and returns its claims.
     *
     * @param token the JWT string
     * @return parsed claims
     * @throws JwtException if the token is invalid, expired, or tampered
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user ID (subject) from a valid token.
     */
    public Integer getUserId(String token) {
        return Integer.valueOf(validateToken(token).getSubject());
    }

    /**
     * Extracts the workspace ID from a valid token.
     */
    public Integer getWorkspaceId(String token) {
        return validateToken(token).get("workspaceId", Integer.class);
    }
}
