package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final AppProperties appProperties;

    /**
     * Generates a password reset token for the given username.
     * Token expires after the configured password-reset-expiration time (default: 2
     * hours).
     *
     * @param username the username to generate the token for
     * @return JWT token string
     */
    public String generatePasswordResetToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        long expirationTime = appProperties.getJwt().getPasswordResetExpiration();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getPasswordResetSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates the password reset token and extracts the username.
     *
     * @param token the JWT token to validate
     * @return the username from the token
     * @throws JwtException if the token is invalid or expired
     */
    public String validateTokenAndGetUsername(String token) {
        Claims claims = extractAllClaims(token);

        if (isTokenExpired(token)) {
            throw new JwtException("Password reset token has expired");
        }

        return claims.getSubject();
    }

    /**
     * Checks if the token is expired.
     *
     * @param token the JWT token to check
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getPasswordResetSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getPasswordResetSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getPasswordResetSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
