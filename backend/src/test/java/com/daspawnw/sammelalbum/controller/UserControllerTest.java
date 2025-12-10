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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.model.*;
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

        @Autowired
        private com.daspawnw.sammelalbum.repository.StickerRepository stickerRepository;

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
        void getMe_ShouldReturn401_WhenUnauthenticated() throws Exception {
                mockMvc.perform(get("/api/user/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteMe_ShouldDeleteUser_WhenAuthenticated() throws Exception {
                // Given: Create user with offers and searches
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

                // Create stickers
                Sticker sticker1 = Sticker.builder().id(1L).name("Sticker 1").build();
                Sticker sticker2 = Sticker.builder().id(2L).name("Sticker 2").build();
                stickerRepository.save(sticker1);
                stickerRepository.save(sticker2);

                // Create offers and searches
                CardOffer offer = CardOffer.builder()
                                .userId(userId)
                                .stickerId(sticker1.getId())
                                .offerFreebie(true)
                                .isReserved(false)
                                .build();
                cardOfferRepository.save(offer);

                CardSearch search = CardSearch.builder()
                                .userId(userId)
                                .stickerId(sticker2.getId())
                                .isReserved(false)
                                .build();
                cardSearchRepository.save(search);

                CustomUserDetails userDetails = new CustomUserDetails(
                                "testuser", "password", Collections.emptyList(), userId);
                String token = "Bearer " + jwtService.generateToken(userDetails, userId);

                // When: Delete user
                mockMvc.perform(delete("/api/user/me")
                                .header("Authorization", token))
                                .andExpect(status().isNoContent());

                // Then: Verify user and all data is deleted
                assertFalse(userRepository.existsById(userId));
                assertFalse(credentialsRepository.findByUserId(userId).isPresent());
                assertEquals(0, cardOfferRepository.findAllByUserId(userId).size());
                assertEquals(0, cardSearchRepository.findAllByUserId(userId).size());
        }

        @Test
        void deleteMe_ShouldReturn401_WhenUnauthenticated() throws Exception {
                mockMvc.perform(delete("/api/user/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteMe_ShouldCloseExchanges_WhenUserHasActiveExchanges() throws Exception {
                // Given: Create two users
                User user1 = User.builder()
                                .firstname("User")
                                .lastname("One")
                                .mail("user1@example.com")
                                .build();

                Credentials credentials1 = Credentials.builder()
                                .username("user1")
                                .passwordHash(passwordEncoder.encode("password"))
                                .user(user1)
                                .build();
                credentialsRepository.save(credentials1);
                Long userId1 = credentials1.getUser().getId();

                User user2 = User.builder()
                                .firstname("User")
                                .lastname("Two")
                                .mail("user2@example.com")
                                .build();

                Credentials credentials2 = Credentials.builder()
                                .username("user2")
                                .passwordHash(passwordEncoder.encode("password"))
                                .user(user2)
                                .build();
                credentialsRepository.save(credentials2);
                Long userId2 = credentials2.getUser().getId();

                // Create stickers
                Sticker sticker1 = Sticker.builder().id(1L).name("Sticker 1").build();
                stickerRepository.save(sticker1);

                // Create offers and searches
                CardOffer user1Offer = CardOffer.builder()
                                .userId(userId1)
                                .stickerId(sticker1.getId())
                                .offerFreebie(true)
                                .isReserved(true)
                                .build();
                cardOfferRepository.save(user1Offer);

                CardSearch user2Search = CardSearch.builder()
                                .userId(userId2)
                                .stickerId(sticker1.getId())
                                .isReserved(true)
                                .build();
                cardSearchRepository.save(user2Search);

                // Create exchange in EXCHANGE_INTERREST status
                ExchangeRequest exchange = ExchangeRequest.builder()
                                .requesterId(userId2)
                                .offererId(userId1)
                                .requestedStickerId(sticker1.getId())
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(user1Offer.getId())
                                .requesterCardSearchId(user2Search.getId())
                                .build();
                exchangeRequestRepository.save(exchange);

                CustomUserDetails userDetails = new CustomUserDetails(
                                "user1", "password", Collections.emptyList(), userId1);
                String token = "Bearer " + jwtService.generateToken(userDetails, userId1);

                // When: Delete user1
                mockMvc.perform(delete("/api/user/me")
                                .header("Authorization", token))
                                .andExpect(status().isNoContent());

                // Then: Verify exchange is deleted and user2's search is unreserved
                assertFalse(exchangeRequestRepository.findById(exchange.getId()).isPresent());

                CardSearch unreservedSearch = cardSearchRepository.findById(user2Search.getId()).orElseThrow();
                assertFalse(unreservedSearch.getIsReserved());

                // Verify user1 is deleted
                assertFalse(userRepository.existsById(userId1));
        }
}
