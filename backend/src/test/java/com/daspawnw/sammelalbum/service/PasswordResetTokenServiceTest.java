package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.config.AppProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.JwtProperties jwtProperties;

    private PasswordResetTokenService passwordResetTokenService;

    private static final String TEST_SECRET = "R9mPX2vYq8wB5nZt7cKjH4fL6gD3sA1eU0iO9pMxN8y=";
    private static final long EXPIRATION_TIME = 7200000L; // 2 hours

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.getJwt()).thenReturn(jwtProperties);
        lenient().when(jwtProperties.getPasswordResetSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtProperties.getPasswordResetExpiration()).thenReturn(EXPIRATION_TIME);

        passwordResetTokenService = new PasswordResetTokenService(appProperties);
    }

    @Test
    void generatePasswordResetToken_shouldGenerateValidToken() {
        // Given
        String username = "testuser";

        // When
        String token = passwordResetTokenService.generatePasswordResetToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // Verify token can be validated
        String extractedUsername = passwordResetTokenService.validateTokenAndGetUsername(token);
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void validateTokenAndGetUsername_shouldReturnUsername_whenTokenIsValid() {
        // Given
        String username = "testuser";
        String token = passwordResetTokenService.generatePasswordResetToken(username);

        // When
        String extractedUsername = passwordResetTokenService.validateTokenAndGetUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void validateTokenAndGetUsername_shouldThrowException_whenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThatThrownBy(() -> passwordResetTokenService.validateTokenAndGetUsername(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateTokenAndGetUsername_shouldThrowException_whenTokenIsExpired() {
        // Given - create a token with very short expiration
        when(jwtProperties.getPasswordResetExpiration()).thenReturn(1L); // 1ms
        PasswordResetTokenService shortExpirationService = new PasswordResetTokenService(appProperties);
        String token = shortExpirationService.generatePasswordResetToken("testuser");

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThatThrownBy(() -> shortExpirationService.validateTokenAndGetUsername(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void isTokenExpired_shouldReturnFalse_whenTokenIsValid() {
        // Given
        String token = passwordResetTokenService.generatePasswordResetToken("testuser");

        // When
        boolean isExpired = passwordResetTokenService.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_shouldReturnTrue_whenTokenIsExpired() {
        // Given - create a token with very short expiration
        when(jwtProperties.getPasswordResetExpiration()).thenReturn(1L); // 1ms
        PasswordResetTokenService shortExpirationService = new PasswordResetTokenService(appProperties);
        String token = shortExpirationService.generatePasswordResetToken("testuser");

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isExpired = shortExpirationService.isTokenExpired(token);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    void isTokenExpired_shouldReturnTrue_whenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isExpired = passwordResetTokenService.isTokenExpired(invalidToken);

        // Then
        assertThat(isExpired).isTrue();
    }
}
