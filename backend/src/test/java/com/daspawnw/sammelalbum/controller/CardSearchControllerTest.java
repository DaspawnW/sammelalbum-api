package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.CardSearchDtos.BulkCardSearchRequest;
import com.daspawnw.sammelalbum.dto.CardSearchDtos.CardSearchRequest;
import com.daspawnw.sammelalbum.model.CardSearch;
import com.daspawnw.sammelalbum.model.Sticker;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import com.daspawnw.sammelalbum.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
                "app.validation-codes=CODE1,CODE2",
                "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CardSearchControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private CardSearchRepository cardSearchRepository;

        @Autowired
        private StickerRepository stickerRepository;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private CredentialsRepository credentialsRepository;
        @Autowired
        private PasswordEncoder passwordEncoder;

        private String aliceToken;
        private String bobToken;
        private Long aliceId;
        private Long bobId;

        @Autowired
        private CardOfferRepository cardOfferRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.ExchangeRequestRepository exchangeRequestRepository;

        @BeforeEach
        void setup() {
                exchangeRequestRepository.deleteAll();
                cardSearchRepository.deleteAll();
                cardOfferRepository.deleteAll();
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

                // Create Bob
                var bob = User.builder()
                                .firstname("Bob").lastname("Doe").mail("bob@example.com").build();
                var bobCreds = Credentials.builder()
                                .username("bob").passwordHash(passwordEncoder.encode("password")).user(bob).build();
                credentialsRepository.save(bobCreds);
                bobId = bobCreds.getUser().getId();

                var bobUserDetails = new CustomUserDetails(
                                "bob", "password", java.util.Collections.emptyList(), bobId);
                bobToken = "Bearer " + jwtService.generateToken(bobUserDetails, bobId);

                // Create Stickers
                stickerRepository.save(Sticker.builder().id(1L).name("Sticker 1").build());
                stickerRepository.save(Sticker.builder().id(2L).name("Sticker 2").build());
                stickerRepository.save(Sticker.builder().id(3L).name("Sticker 3").build());
                stickerRepository.save(Sticker.builder().id(222L).name("Sticker 222").build());
                stickerRepository.save(Sticker.builder().id(233L).name("Sticker 233").build());
        }

        @Test
        void addSearch_Success() throws Exception {
                CardSearchRequest request = new CardSearchRequest(1L);

                mockMvc.perform(post("/api/card-searches")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.stickerId").value(1))
                                .andExpect(jsonPath("$.stickerName").value("Sticker 1"));

                assertEquals(1, cardSearchRepository.findAllByUserId(aliceId).size());
        }

        @Test
        void getSearches_Success() throws Exception {
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(1L).build());
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(2L).build());
                cardSearchRepository.save(CardSearch.builder().userId(bobId).stickerId(3L).build());

                mockMvc.perform(get("/api/card-searches")
                                .header("Authorization", aliceToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].stickerName").value("Sticker 1"))
                                .andExpect(jsonPath("$[1].stickerName").value("Sticker 2"));
        }

        @Test
        void deleteSearch_Success() throws Exception {
                CardSearch search = cardSearchRepository
                                .save(CardSearch.builder().userId(aliceId).stickerId(1L).build());

                mockMvc.perform(delete("/api/card-searches/" + search.getId())
                                .header("Authorization", aliceToken))
                                .andExpect(status().isNoContent());

                assertEquals(0, cardSearchRepository.findAllByUserId(aliceId).size());
        }

        @Test
        void deleteSearch_Forbidden() throws Exception {
                CardSearch search = cardSearchRepository.save(CardSearch.builder().userId(bobId).stickerId(1L).build());

                mockMvc.perform(delete("/api/card-searches/" + search.getId())
                                .header("Authorization", aliceToken))
                                .andExpect(status().isForbidden());

                assertEquals(1, cardSearchRepository.findAllByUserId(bobId).size());
        }

        @Test
        void bulkAdd_Success() throws Exception {
                BulkCardSearchRequest request = new BulkCardSearchRequest(Arrays.asList(1L, 2L, 1L));

                mockMvc.perform(post("/api/card-searches/bulk")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(3)));

                assertEquals(3, cardSearchRepository.findAllByUserId(aliceId).size());
        }

        @Test
        void bulkDelete_PartialDuplicates() throws Exception {
                // Alice has 233, 222, 233
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(233L).build());
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(222L).build());
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(233L).build());

                // Delete 233, 222
                BulkCardSearchRequest request = new BulkCardSearchRequest(Arrays.asList(233L, 222L));

                mockMvc.perform(post("/api/card-searches/bulk-delete")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                List<CardSearch> remaining = cardSearchRepository.findAllByUserId(aliceId);
                assertEquals(1, remaining.size());
                assertEquals(233L, remaining.get(0).getStickerId());
        }

        @Test
        void bulkDelete_AllDuplicates() throws Exception {
                // Alice has 233, 222, 233
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(233L).build());
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(222L).build());
                cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(233L).build());

                // Delete 233, 222, 233
                BulkCardSearchRequest request = new BulkCardSearchRequest(Arrays.asList(233L, 222L, 233L));

                mockMvc.perform(post("/api/card-searches/bulk-delete")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                List<CardSearch> remaining = cardSearchRepository.findAllByUserId(aliceId);
                assertEquals(0, remaining.size());
        }
}
