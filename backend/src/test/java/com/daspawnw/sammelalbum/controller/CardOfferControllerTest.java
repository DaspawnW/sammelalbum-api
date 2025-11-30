package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkCardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkUpdateOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferRequest;
import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class CardOfferControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private CardOfferRepository cardOfferRepository;

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
        @Autowired
        private com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;
        @Autowired
        private com.daspawnw.sammelalbum.repository.ExchangeRequestRepository exchangeRequestRepository;
        @Autowired
        private com.daspawnw.sammelalbum.repository.StickerRepository stickerRepository;

        private String aliceToken;
        private String bobToken;
        private Long aliceId;
        private Long bobId;

        @BeforeEach
        void setup() {
                exchangeRequestRepository.deleteAll();
                cardSearchRepository.deleteAll();
                cardOfferRepository.deleteAll();
                credentialsRepository.deleteAll();
                userRepository.deleteAll();
                stickerRepository.deleteAll();

                // Create Stickers
                stickerRepository.save(
                                com.daspawnw.sammelalbum.model.Sticker.builder().id(1L).name("Sticker 1").build());
                stickerRepository.save(
                                com.daspawnw.sammelalbum.model.Sticker.builder().id(2L).name("Sticker 2").build());
                stickerRepository.save(
                                com.daspawnw.sammelalbum.model.Sticker.builder().id(3L).name("Sticker 3").build());
                stickerRepository.save(
                                com.daspawnw.sammelalbum.model.Sticker.builder().id(222L).name("Sticker 222").build());
                stickerRepository.save(
                                com.daspawnw.sammelalbum.model.Sticker.builder().id(233L).name("Sticker 233").build());

                // Create Alice
                var alice = User.builder()
                                .firstname("Alice").lastname("Doe").mail("alice@example.com").build();
                var aliceCreds = Credentials.builder()
                                .username("alice").passwordHash(passwordEncoder.encode("password")).user(alice).build();
                credentialsRepository.save(aliceCreds);
                aliceId = aliceCreds.getUser().getId();

                var aliceUserDetails = new CustomUserDetails(
                                "alice", "password", Collections.emptyList(), aliceId);
                aliceToken = "Bearer " + jwtService.generateToken(aliceUserDetails, aliceId);

                // Create Bob
                var bob = User.builder()
                                .firstname("Bob").lastname("Doe").mail("bob@example.com").build();
                var bobCreds = Credentials.builder()
                                .username("bob").passwordHash(passwordEncoder.encode("password")).user(bob).build();
                credentialsRepository.save(bobCreds);
                bobId = bobCreds.getUser().getId();

                var bobUserDetails = new CustomUserDetails(
                                "bob", "password", Collections.emptyList(), bobId);
                bobToken = "Bearer " + jwtService.generateToken(bobUserDetails, bobId);
        }

        @Test
        void addOffer_Success() throws Exception {
                CardOfferRequest request = new CardOfferRequest(1L, true, false, false);

                mockMvc.perform(post("/api/card-offers")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.stickerId").value(1))
                                .andExpect(jsonPath("$.stickerName").value("Sticker 1"))
                                .andExpect(jsonPath("$.offerPayed").value(true))
                                .andExpect(jsonPath("$.offerFreebie").value(false))
                                .andExpect(jsonPath("$.offerExchange").value(false));

                assertEquals(1, cardOfferRepository.findAllByUserId(aliceId).size());
        }

        @Test
        void getOffers_Success() throws Exception {
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(1L).offerPayed(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(2L).offerExchange(true).build());
                cardOfferRepository.save(CardOffer.builder().userId(bobId).stickerId(3L).offerFreebie(true).build());

                mockMvc.perform(get("/api/card-offers")
                                .header("Authorization", aliceToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].stickerName").value("Sticker 1"))
                                .andExpect(jsonPath("$[1].stickerName").value("Sticker 2"));
        }

        @Test
        void deleteOffer_Success() throws Exception {
                CardOffer offer = cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(1L).offerPayed(true).build());

                mockMvc.perform(delete("/api/card-offers/" + offer.getId())
                                .header("Authorization", aliceToken))
                                .andExpect(status().isNoContent());

                assertEquals(0, cardOfferRepository.findAllByUserId(aliceId).size());
        }

        @Test
        void deleteOffer_Forbidden() throws Exception {
                CardOffer offer = cardOfferRepository
                                .save(CardOffer.builder().userId(bobId).stickerId(1L).offerPayed(true).build());

                mockMvc.perform(delete("/api/card-offers/" + offer.getId())
                                .header("Authorization", aliceToken))
                                .andExpect(status().isForbidden());

                assertEquals(1, cardOfferRepository.findAllByUserId(bobId).size());
        }

        @Test
        void bulkAdd_Success() throws Exception {
                BulkCardOfferRequest request = new BulkCardOfferRequest(Arrays.asList(1L, 2L, 1L), false, true, false);

                mockMvc.perform(post("/api/card-offers/bulk")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(3)))
                                .andExpect(jsonPath("$[0].offerFreebie").value(true));

                assertEquals(3, cardOfferRepository.findAllByUserId(aliceId).size());
        }

        @Test
        void bulkDelete_PartialDuplicates() throws Exception {
                // Alice has 233, 222, 233
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(233L).offerPayed(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(222L).offerPayed(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(233L).offerPayed(true).build());

                // Delete 233, 222
                BulkCardOfferRequest request = new BulkCardOfferRequest(Arrays.asList(233L, 222L), null, null, null);

                mockMvc.perform(post("/api/card-offers/bulk-delete")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                List<CardOffer> remaining = cardOfferRepository.findAllByUserId(aliceId);
                assertEquals(1, remaining.size());
                assertEquals(233L, remaining.get(0).getStickerId());
        }

        @Test
        void bulkUpdate_Success() throws Exception {
                // Alice has 233 (FREEBIE), 222 (EXCHANGE), 233 (FREEBIE)
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(233L).offerFreebie(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(222L).offerExchange(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(aliceId).stickerId(233L).offerFreebie(true).build());

                // Update 233 to PAYED
                BulkUpdateOfferRequest request = new BulkUpdateOfferRequest(Arrays.asList(233L), true, false, false);

                mockMvc.perform(put("/api/card-offers/bulk")
                                .header("Authorization", aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2))) // Should return the 2 updated offers
                                .andExpect(jsonPath("$[0].offerPayed").value(true))
                                .andExpect(jsonPath("$[0].offerFreebie").value(false))
                                .andExpect(jsonPath("$[1].offerPayed").value(true));

                // Verify DB
                List<CardOffer> offers = cardOfferRepository.findAllByUserId(aliceId);
                assertEquals(3, offers.size());
                long payedCount = offers.stream().filter(o -> o.getOfferPayed()).count();
                assertEquals(2, payedCount);
                long exchangeCount = offers.stream().filter(o -> o.getOfferExchange()).count();
                assertEquals(1, exchangeCount);
        }
}
