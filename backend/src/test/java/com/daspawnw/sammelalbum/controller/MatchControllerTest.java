package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.MatchDtos.MatchResponse;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.JwtService;
import com.daspawnw.sammelalbum.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
                "app.validation-codes=CODE1,CODE2",
                "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MatchControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MatchService matchService;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private CredentialsRepository credentialsRepository;
        @Autowired
        private PasswordEncoder passwordEncoder;
        @Autowired
        private CardOfferRepository cardOfferRepository;
        @Autowired
        private CardSearchRepository cardSearchRepository;
        @Autowired
        private StickerRepository stickerRepository;

        private String aliceToken;
        private Long aliceId;

        @BeforeEach
        void setup() {
                cardOfferRepository.deleteAll();
                cardSearchRepository.deleteAll();
                credentialsRepository.deleteAll();
                userRepository.deleteAll();
                stickerRepository.deleteAll();

                // Create Alice
                var alice = User.builder()
                                .firstname("Alice").lastname("Doe").mail("alice@example.com").build();
                var aliceCreds = Credentials.builder()
                                .username("alice").passwordHash(passwordEncoder.encode("password")).user(alice).build();
                credentialsRepository.save(aliceCreds);
                aliceId = aliceCreds.getUser().getId();

                var aliceUserDetails = new CustomUserDetails(
                                "alice", "password", java.util.Collections.emptyList(), aliceId);
                aliceToken = "Bearer " + jwtService.generateToken(aliceUserDetails, aliceId);
        }

        @Test
        void getFreebieMatches_Success() throws Exception {
                MatchResponse response = new MatchResponse(2L, 5L, Collections.emptyList());
                Page<MatchResponse> page = new PageImpl<>(List.of(response));

                when(matchService.getFreebieMatches(eq(aliceId), any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/api/matches/freebie")
                                .header("Authorization", aliceToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].userId").value(2))
                                .andExpect(jsonPath("$.content[0].matchCount").value(5))
                                .andExpect(jsonPath("$.content[0].matches").isArray());
        }

        @Test
        void getPayedMatches_Success() throws Exception {
                MatchResponse response = new MatchResponse(3L, 3L, Collections.emptyList());
                Page<MatchResponse> page = new PageImpl<>(List.of(response));

                when(matchService.getPayedMatches(eq(aliceId), any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/api/matches/payed")
                                .header("Authorization", aliceToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].userId").value(3))
                                .andExpect(jsonPath("$.content[0].matchCount").value(3))
                                .andExpect(jsonPath("$.content[0].matches").isArray());
        }

        @Test
        void getExchangeMatches_Success() throws Exception {
                MatchResponse response = new MatchResponse(4L, 1L, Collections.emptyList());
                Page<MatchResponse> page = new PageImpl<>(List.of(response));

                when(matchService.getExchangeMatches(eq(aliceId), any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/api/matches/exchange")
                                .header("Authorization", aliceToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].userId").value(4))
                                .andExpect(jsonPath("$.content[0].matchCount").value(1))
                                .andExpect(jsonPath("$.content[0].matches").isArray());
        }
}
