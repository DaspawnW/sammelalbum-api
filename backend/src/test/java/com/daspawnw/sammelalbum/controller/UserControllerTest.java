package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.ChangePasswordRequest;
import com.daspawnw.sammelalbum.dto.UpdateProfileRequest;
import com.daspawnw.sammelalbum.model.Credentials;

import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.daspawnw.sammelalbum.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daspawnw.sammelalbum.security.CustomUserDetails;
import java.util.Collections;

@SpringBootTest(properties = {
                "app.validation-codes=CODE-1111",
                "app.jwt.secret=K7gNU3kef8297wnsJvbdw/Ba49bmGW76NFh70fE0ZeM=",
                "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.CredentialsRepository credentialsRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private com.daspawnw.sammelalbum.repository.CardOfferRepository cardOfferRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.EmailOutboxRepository emailOutboxRepository;

        @BeforeEach
        void setUp() {
                emailOutboxRepository.deleteAll();
                exchangeRequestRepository.deleteAll();
                cardOfferRepository.deleteAll();
                cardSearchRepository.deleteAll();
                credentialsRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void getMe_ShouldReturnCurrentUser_WhenAuthenticated() throws Exception {
                User user = User.builder()
                                .firstname("Test")
                                .lastname("User")
                                .mail("test@example.com")
                                .contact("test@contact.com")
                                .build();

                Credentials credentials = Credentials.builder()
                                .username("testuser")
                                .passwordHash(passwordEncoder.encode("password"))
                                .user(user)
                                .build();
                credentialsRepository.save(credentials);
                user = credentials.getUser(); // Get saved user with ID

                CustomUserDetails userDetails = new CustomUserDetails(
                                "testuser",
                                "password",
                                Collections.emptyList(),
                                user.getId());

                String token = "Bearer " + jwtService.generateToken(userDetails, user.getId());

                mockMvc.perform(get("/api/user/me")
                                .header("Authorization", token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstname").value("Test"))
                                .andExpect(jsonPath("$.lastname").value("User"))
                                .andExpect(jsonPath("$.mail").value("test@example.com"))
                                .andExpect(jsonPath("$.contact").value("test@contact.com"));
        }

        @Test
        void updateProfile_ShouldUpdateProfile_WhenRequestIsValid() throws Exception {
                User user = User.builder()
                                .firstname("Test")
                                .lastname("User")
                                .mail("test@example.com")
                                .contact("OldContact")
                                .build();

                Credentials credentials = Credentials.builder()
                                .username("testuser")
                                .passwordHash(passwordEncoder.encode("password"))
                                .user(user)
                                .build();
                credentialsRepository.save(credentials);
                Long userId = credentials.getUser().getId();

                CustomUserDetails userDetails = new CustomUserDetails(
                                "testuser", "password", Collections.emptyList(), userId);
                String token = "Bearer " + jwtService.generateToken(userDetails, userId);

                UpdateProfileRequest request = UpdateProfileRequest.builder()
                                .firstname("UpdatedFirst")
                                .lastname("UpdatedLast")
                                .mail("updated@example.com")
                                .contact("UpdatedContact")
                                .build();

                mockMvc.perform(put("/api/user/profile")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                User updatedUser = userRepository.findById(userId).orElseThrow();
                assertEquals("UpdatedFirst", updatedUser.getFirstname());
                assertEquals("UpdatedLast", updatedUser.getLastname());
                assertEquals("updated@example.com", updatedUser.getMail());
                assertEquals("UpdatedContact", updatedUser.getContact());
        }

        @Test
        void changePassword_ShouldChangePassword_WhenCurrentPasswordIsCorrect() throws Exception {
                User user = User.builder()
                                .firstname("Test")
                                .lastname("User")
                                .mail("test@example.com")
                                .build();

                Credentials credentials = Credentials.builder()
                                .username("testuser")
                                .passwordHash(passwordEncoder.encode("password"))
                                .user(user)
                                .build();
                credentialsRepository.save(credentials);
                Long userId = credentials.getUser().getId();

                CustomUserDetails userDetails = new CustomUserDetails(
                                "testuser", "password", Collections.emptyList(), userId);
                String token = "Bearer " + jwtService.generateToken(userDetails, userId);

                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("password")
                                .newPassword("newpassword")
                                .build();

                mockMvc.perform(put("/api/user/password")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                Credentials updatedCredentials = credentialsRepository.findByUserId(userId).orElseThrow();
                assertTrue(passwordEncoder.matches("newpassword", updatedCredentials.getPasswordHash()));
        }

        @Test
        void getMe_ShouldReturn403_WhenUnauthenticated() throws Exception {
                mockMvc.perform(get("/api/user/me"))
                                .andExpect(status().isForbidden());
        }
}
