package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.config.AppProperties;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.notification.NotificationService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final CredentialsRepository credentialsRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    /**
     * Initiates the password reset process by generating a token and sending an
     * email.
     * For security reasons, this method does not reveal whether the user exists or
     * not.
     *
     * @param identifier username or email address
     */
    @Transactional
    public void requestPasswordReset(String identifier) {
        // Try to find user by username or email
        Optional<Credentials> credentialsOpt = credentialsRepository.findByUsername(identifier);

        if (credentialsOpt.isEmpty()) {
            // Try to find by email
            Optional<User> userOpt = userRepository.findByMail(identifier);
            if (userOpt.isPresent()) {
                credentialsOpt = credentialsRepository.findByUserId(userOpt.get().getId());
            }
        }

        // If user exists, generate token and send email
        if (credentialsOpt.isPresent()) {
            Credentials credentials = credentialsOpt.get();
            User user = credentials.getUser();
            String username = credentials.getUsername();

            // Generate password reset token
            String resetToken = passwordResetTokenService.generatePasswordResetToken(username);

            // Create reset URL
            String resetUrl = String.format("%s/password-reset?token=%s",
                    appProperties.getBaseUrl(), resetToken);

            // Build bilingual email message
            String emailBody = buildPasswordResetEmailBody(user.getFirstname(), resetUrl);

            // Send notification using NotificationService
            notificationService.sendPasswordResetNotification(user.getId(), emailBody);

            log.info("Password reset email queued for user: {}", username);
        } else {
            // For security, don't reveal that the user doesn't exist
            log.info("Password reset requested for non-existent identifier: {}", identifier);
        }
    }

    /**
     * Resets the password using a valid password reset token.
     *
     * @param token       the password reset JWT token
     * @param newPassword the new password
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        try {
            // Validate token and extract username
            String username = passwordResetTokenService.validateTokenAndGetUsername(token);

            // Find credentials
            Credentials credentials = credentialsRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Update password
            credentials.setPasswordHash(passwordEncoder.encode(newPassword));
            credentialsRepository.save(credentials);

            log.info("Password successfully reset for user: {}", username);

        } catch (JwtException e) {
            log.warn("Invalid or expired password reset token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired password reset token", e);
        }
    }

    private String buildPasswordResetEmailBody(String firstname, String resetUrl) {
        // German message
        String deMessage = String.format(
                "Hallo %s,\n\n" +
                        "Wir haben eine Anfrage zum Zurücksetzen deines Passworts erhalten. " +
                        "Klicke auf den folgenden Link, um dein Passwort zurückzusetzen:\n\n" +
                        "%s\n\n" +
                        "Dieser Link läuft in 2 Stunden ab.\n\n" +
                        "Falls du diese Anfrage nicht gestellt hast, ignoriere bitte diese E-Mail.\n\n" +
                        "Viele Grüße,\n" +
                        "Das Sammelalbum Team",
                firstname, resetUrl);

        // English message
        String enMessage = String.format(
                "Hello %s,\n\n" +
                        "We received a request to reset your password. " +
                        "Click the link below to reset your password:\n\n" +
                        "%s\n\n" +
                        "This link will expire in 2 hours.\n\n" +
                        "If you didn't request a password reset, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The Sammelalbum Team",
                firstname, resetUrl);

        // Combine German and English (German first, as per ExchangeService pattern)
        return deMessage + "\n\n" + enMessage;
    }
}
