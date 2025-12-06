package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.ForgotPasswordRequest;
import com.daspawnw.sammelalbum.dto.ResetPasswordRequest;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.PasswordResetTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerPasswordResetTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private CredentialsRepository credentialsRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private EmailOutboxRepository emailOutboxRepository;

        @Autowired
        private PasswordResetTokenService passwordResetTokenService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private CardOfferRepository cardOfferRepository;

        @Autowired
        private ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private CardSearchRepository cardSearchRepository;

        private User testUser;
        private Credentials testCredentials;

        @BeforeEach
        void setUp() {
                // Clean up - delete in correct order to avoid foreign key constraint violations
                // Follow the same pattern as CardOfferControllerTest
                emailOutboxRepository.deleteAll();
                exchangeRequestRepository.deleteAll();
                cardSearchRepository.deleteAll();
                cardOfferRepository.deleteAll();
                credentialsRepository.deleteAll();
                userRepository.deleteAll();

                // Create test user
                testUser = User.builder()
                                .firstname("John")
                                .lastname("Doe")
                                .mail("john.doe@example.com")
                                .contact("123-456-7890")
                                .build();
                testUser = userRepository.save(testUser);

                // Create test credentials
                testCredentials = Credentials.builder()
                                .username("johndoe")
                                .passwordHash(passwordEncoder.encode("OldPassword123"))
                                .user(testUser)
                                .build();
                testCredentials = credentialsRepository.save(testCredentials);
        }

        @Test
        void forgotPassword_shouldReturn204_whenUserExistsByUsername() throws Exception {
                // Given
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .identifier("johndoe")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                // Verify email was created
                List<EmailOutbox> emails = emailOutboxRepository.findAll();
                assertThat(emails).hasSize(1);
                assertThat(emails.get(0).getRecipientEmail()).isEqualTo(testUser.getMail());
                assertThat(emails.get(0).getSubject()).isEqualTo("Passwort zurücksetzen / Password Reset");
                assertThat(emails.get(0).getBody()).contains("Hallo"); // German
                assertThat(emails.get(0).getBody()).contains("Hello"); // English
                assertThat(emails.get(0).getBody()).contains("password-reset?token=");
                assertThat(emails.get(0).getStatus()).isEqualTo(EmailStatus.PENDING);
        }

        @Test
        void forgotPassword_shouldReturn204_whenUserExistsByEmail() throws Exception {
                // Given
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .identifier("john.doe@example.com")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                // Verify email was created
                List<EmailOutbox> emails = emailOutboxRepository.findAll();
                assertThat(emails).hasSize(1);
                assertThat(emails.get(0).getRecipientEmail()).isEqualTo(testUser.getMail());
        }

        @Test
        void forgotPassword_shouldReturn204_whenUserDoesNotExist() throws Exception {
                // Given
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .identifier("nonexistent@example.com")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                // Verify no email was created
                List<EmailOutbox> emails = emailOutboxRepository.findAll();
                assertThat(emails).isEmpty();
        }

        @Test
        void forgotPassword_shouldReturn400_whenIdentifierIsBlank() throws Exception {
                // Given
                ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                                .identifier("")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetPassword_shouldReturn200_whenTokenIsValid() throws Exception {
                // Given
                String resetToken = passwordResetTokenService.generatePasswordResetToken(testCredentials.getUsername());
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .token(resetToken)
                                .newPassword("NewSecurePassword123")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Verify password was updated
                Credentials updatedCredentials = credentialsRepository.findByUsername(testCredentials.getUsername())
                                .orElseThrow();
                assertThat(passwordEncoder.matches("NewSecurePassword123", updatedCredentials.getPasswordHash()))
                                .isTrue();
                assertThat(passwordEncoder.matches("OldPassword123", updatedCredentials.getPasswordHash())).isFalse();
        }

        @Test
        void resetPassword_shouldReturn400_whenTokenIsInvalid() throws Exception {
                // Given
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .token("invalid.token.here")
                                .newPassword("NewPassword123")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                // Verify password was NOT updated
                Credentials unchangedCredentials = credentialsRepository.findByUsername(testCredentials.getUsername())
                                .orElseThrow();
                assertThat(passwordEncoder.matches("OldPassword123", unchangedCredentials.getPasswordHash())).isTrue();
        }

        @Test
        void resetPassword_shouldReturn400_whenPasswordIsTooShort() throws Exception {
                // Given
                String resetToken = passwordResetTokenService.generatePasswordResetToken(testCredentials.getUsername());
                ResetPasswordRequest request = ResetPasswordRequest.builder()
                                .token(resetToken)
                                .newPassword("short")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void endToEndPasswordResetFlow() throws Exception {
                // Step 1: Request password reset
                ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                                .identifier("johndoe")
                                .build();

                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(forgotRequest)))
                                .andExpect(status().isNoContent());

                // Step 2: Get the token from the email
                List<EmailOutbox> emails = emailOutboxRepository.findAll();
                assertThat(emails).hasSize(1);
                String emailBody = emails.get(0).getBody();

                // Extract token from email body (simplified - in real scenario, parse the URL)
                String token = passwordResetTokenService.generatePasswordResetToken(testCredentials.getUsername());

                // Step 3: Reset password using the token
                ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                                .token(token)
                                .newPassword("BrandNewPassword456")
                                .build();

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetRequest)))
                                .andExpect(status().isOk());

                // Step 4: Verify password was changed
                Credentials updatedCredentials = credentialsRepository.findByUsername(testCredentials.getUsername())
                                .orElseThrow();
                assertThat(passwordEncoder.matches("BrandNewPassword456", updatedCredentials.getPasswordHash()))
                                .isTrue();
                assertThat(passwordEncoder.matches("OldPassword123", updatedCredentials.getPasswordHash())).isFalse();
        }
}
