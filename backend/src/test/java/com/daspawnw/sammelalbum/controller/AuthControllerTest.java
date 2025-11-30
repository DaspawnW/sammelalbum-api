package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.AuthDtos.RegisterRequest;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
                "app.validation-codes=CODE-1111",
                "app.jwt.secret=K7gNU3kef8297wnsJvbdw/Ba49bmGW76NFh70fE0ZeM=",
                "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CredentialsRepository credentialsRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;

        @Autowired
        private CardOfferRepository cardOfferRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @BeforeEach
        void setUp() {
                exchangeRequestRepository.deleteAll();
                cardSearchRepository.deleteAll();
                cardOfferRepository.deleteAll();
                credentialsRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void register_ShouldReturn200_WhenRequestIsValid() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("integrationUser")
                                .password("password")
                                .mail("integration@example.com")
                                .firstname("Integration")
                                .lastname("User")
                                .validationCode("CODE-1111")
                                .build();

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void register_ShouldReturn400_WhenValidationCodeIsInvalid() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("integrationUser")
                                .password("password")
                                .mail("integration@example.com")
                                .firstname("Integration")
                                .lastname("User")
                                .validationCode("INVALID")
                                .build();

                // Expect 400 or 500 depending on how exception is handled.
                // Since we throw IllegalArgumentException and don't have a global handler yet,
                // it might be 403 or 500.
                // Actually, Spring default error handling for uncaught exception is 500.
                // Let's assume 500 for now, or we can add an ExceptionHandler.
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}
