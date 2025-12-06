package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.config.AppProperties;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.notification.NotificationService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

        @Mock
        private CredentialsRepository credentialsRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private NotificationService notificationService;

        @Mock
        private PasswordResetTokenService passwordResetTokenService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private AppProperties appProperties;

        @InjectMocks
        private PasswordResetService passwordResetService;

        private User testUser;
        private Credentials testCredentials;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .firstname("John")
                                .lastname("Doe")
                                .mail("john.doe@example.com")
                                .build();

                testCredentials = Credentials.builder()
                                .id(1L)
                                .username("johndoe")
                                .passwordHash("hashedPassword")
                                .user(testUser)
                                .build();

                lenient().when(appProperties.getBaseUrl()).thenReturn("http://localhost:4200");
        }

        @Test
        void requestPasswordReset_shouldSendEmail_whenUserFoundByUsername() {
                // Given
                String identifier = "johndoe";
                String resetToken = "reset-token-123";

                when(credentialsRepository.findByUsername(identifier)).thenReturn(Optional.of(testCredentials));
                when(passwordResetTokenService.generatePasswordResetToken(testCredentials.getUsername()))
                                .thenReturn(resetToken);

                // When
                passwordResetService.requestPasswordReset(identifier);

                // Then
                ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
                verify(notificationService).sendPasswordResetNotification(eq(testUser.getId()),
                                messageCaptor.capture());

                String emailBody = messageCaptor.getValue();
                assertThat(emailBody).contains("John");
                assertThat(emailBody).contains("http://localhost:4200/password-reset?token=" + resetToken);
                assertThat(emailBody).contains("Hallo"); // German
                assertThat(emailBody).contains("Hello"); // English
        }

        @Test
        void requestPasswordReset_shouldSendEmail_whenUserFoundByEmail() {
                // Given
                String identifier = "john.doe@example.com";
                String resetToken = "reset-token-456";

                when(credentialsRepository.findByUsername(identifier)).thenReturn(Optional.empty());
                when(userRepository.findByMail(identifier)).thenReturn(Optional.of(testUser));
                when(credentialsRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCredentials));
                when(passwordResetTokenService.generatePasswordResetToken(testCredentials.getUsername()))
                                .thenReturn(resetToken);

                // When
                passwordResetService.requestPasswordReset(identifier);

                // Then
                ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
                verify(notificationService).sendPasswordResetNotification(eq(testUser.getId()),
                                messageCaptor.capture());

                String emailBody = messageCaptor.getValue();
                assertThat(emailBody).contains("http://localhost:4200/password-reset?token=" + resetToken);
        }

        @Test
        void requestPasswordReset_shouldNotSendEmail_whenUserNotFound() {
                // Given
                String identifier = "nonexistent";

                when(credentialsRepository.findByUsername(identifier)).thenReturn(Optional.empty());
                when(userRepository.findByMail(identifier)).thenReturn(Optional.empty());

                // When
                passwordResetService.requestPasswordReset(identifier);

                // Then
                verify(notificationService, never()).sendPasswordResetNotification(any(), any());
                verify(passwordResetTokenService, never()).generatePasswordResetToken(anyString());
        }

        @Test
        void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
                // Given
                String token = "valid-token";
                String newPassword = "newSecurePassword123";
                String encodedPassword = "encodedNewPassword";

                when(passwordResetTokenService.validateTokenAndGetUsername(token))
                                .thenReturn(testCredentials.getUsername());
                when(credentialsRepository.findByUsername(testCredentials.getUsername()))
                                .thenReturn(Optional.of(testCredentials));
                when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

                // When
                passwordResetService.resetPassword(token, newPassword);

                // Then
                ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
                verify(credentialsRepository).save(credentialsCaptor.capture());

                Credentials savedCredentials = credentialsCaptor.getValue();
                assertThat(savedCredentials.getPasswordHash()).isEqualTo(encodedPassword);
        }

        @Test
        void resetPassword_shouldThrowException_whenTokenIsInvalid() {
                // Given
                String invalidToken = "invalid-token";
                String newPassword = "newPassword123";

                when(passwordResetTokenService.validateTokenAndGetUsername(invalidToken))
                                .thenThrow(new JwtException("Invalid token"));

                // When & Then
                assertThatThrownBy(() -> passwordResetService.resetPassword(invalidToken, newPassword))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Invalid or expired password reset token");

                verify(credentialsRepository, never()).save(any());
        }

        @Test
        void resetPassword_shouldThrowException_whenTokenIsExpired() {
                // Given
                String expiredToken = "expired-token";
                String newPassword = "newPassword123";

                when(passwordResetTokenService.validateTokenAndGetUsername(expiredToken))
                                .thenThrow(new JwtException("Password reset token has expired"));

                // When & Then
                assertThatThrownBy(() -> passwordResetService.resetPassword(expiredToken, newPassword))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Invalid or expired password reset token");

                verify(credentialsRepository, never()).save(any());
        }

        @Test
        void resetPassword_shouldThrowException_whenUserNotFound() {
                // Given
                String token = "valid-token";
                String newPassword = "newPassword123";

                when(passwordResetTokenService.validateTokenAndGetUsername(token))
                                .thenReturn("nonexistentuser");
                when(credentialsRepository.findByUsername("nonexistentuser"))
                                .thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> passwordResetService.resetPassword(token, newPassword))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("User not found");

                verify(credentialsRepository, never()).save(any());
        }
}
