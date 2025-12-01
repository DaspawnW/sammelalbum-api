package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.model.CardSearch;
import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@org.springframework.transaction.annotation.Transactional
public class ExchangeReservationIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private CardOfferRepository cardOfferRepository;

        @Autowired
        private CardSearchRepository cardSearchRepository;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private jakarta.persistence.EntityManager entityManager;

        private String offererToken;
        private String requesterToken;

        @org.springframework.test.context.transaction.BeforeTransaction
        void cleanup() {
                jdbcTemplate.execute("DELETE FROM email_outbox");
                jdbcTemplate.execute("DELETE FROM exchange_requests");
                jdbcTemplate.execute("DELETE FROM card_searches");
                jdbcTemplate.execute("DELETE FROM card_offers");
                jdbcTemplate.execute("DELETE FROM credentials");
                jdbcTemplate.execute("DELETE FROM users");
                jdbcTemplate.execute("DELETE FROM stickers");
        }

        @BeforeEach
        void setUp() {
                // exchangeRequestRepository.deleteAll(); // Handled by @BeforeTransaction
                // cleanup

                // Ensure credentials exist
                jdbcTemplate.update(
                                "MERGE INTO credentials (user_id, username, password_hash) KEY(user_id) VALUES (2, 'freebie', 'hash')");

                // Offerer is user ID 1 (Main User)
                offererToken = "Bearer "
                                + jwtService.generateToken(
                                                new org.springframework.security.core.userdetails.User("mainuser",
                                                                "password", java.util.Collections.emptyList()),
                                                1L);
                // Requester is user ID 2 (Freebie King)
                requesterToken = "Bearer "
                                + jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                                                "freebie", "password",
                                                java.util.Collections.emptyList()), 2L);

                // Create a request: User 2 requests Sticker 6 (from User 1) offering Sticker 1
                // (from User 2)
                // User 1 offers 6 (EXCHANGE) and needs 1
                // User 2 offers 1 (EXCHANGE) and needs 6
                ExchangeRequest request = ExchangeRequest.builder()
                                .requesterId(2L)
                                .offererId(1L)
                                .requestedStickerId(6L) // Main offers 6 (EXCHANGE)
                                .offeredStickerId(1L) // Freebie offers 1 (FREEBIE in sql, but let's assume EXCHANGE for
                                                      // this test
                                                      // logic or update offer)
                                .exchangeType(com.daspawnw.sammelalbum.model.ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.MAIL_SEND)
                                .build();
                exchangeRequestRepository.save(request);

                // Ensure proper setup for EXCHANGE scenario
                // User 2 offers Sticker 1 as EXCHANGE
                CardOffer offer1 = cardOfferRepository.findByUserIdAndStickerIdIn(2L, List.of(1L)).get(0);
                offer1.setOfferExchange(true);
                cardOfferRepository.save(offer1);

                // User 2 needs Sticker 6 (Should already be in match_scenarios.sql but
                // ensuring)
                if (cardSearchRepository.findByUserIdAndStickerIdIn(2L, List.of(6L)).isEmpty()) {
                        cardSearchRepository.save(CardSearch.builder().userId(2L).stickerId(6L).build());
                }

                // User 1 needs Sticker 1 (Should already be in match_scenarios.sql but
                // ensuring)
                if (cardSearchRepository.findByUserIdAndStickerIdIn(1L, List.of(1L)).isEmpty()) {
                        cardSearchRepository.save(CardSearch.builder().userId(1L).stickerId(1L).build());
                }
        }

        @Test
        void shouldMarkCardsAndSearchesAsReservedOnAccept() throws Exception {
                ExchangeRequest request = exchangeRequestRepository.findAll().get(0);

                // Accept request
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // 1. Requested Sticker (6): Offerer (1) gives, Requester (2) wants
                CardOffer offer6 = cardOfferRepository.findByUserIdAndStickerIdIn(1L, List.of(6L)).get(0);
                assertTrue(offer6.getIsReserved(), "Offerer's card (6) should be reserved");

                CardSearch search6 = cardSearchRepository.findByUserIdAndStickerIdIn(2L, List.of(6L)).get(0);
                assertTrue(search6.getIsReserved(), "Requester's search (6) should be reserved");

                // 2. Offered Sticker (1): Requester (2) gives, Offerer (1) wants
                CardOffer offer1 = cardOfferRepository.findByUserIdAndStickerIdIn(2L, List.of(1L)).get(0);
                assertTrue(offer1.getIsReserved(), "Requester's card (1) should be reserved");

                CardSearch search1 = cardSearchRepository.findByUserIdAndStickerIdIn(1L, List.of(1L)).get(0);
                assertTrue(search1.getIsReserved(), "Offerer's search (1) should be reserved");
        }

        @Test
        void shouldExcludeReservedCardsFromMatchmaking() throws Exception {
                // Initially, User 1 needs 1 (offered by 2) and User 2 needs 6 (offered by 1).
                // This should be a match.

                // Verify match exists initially (User 1 looking for matches)
                // Note: match_scenarios.sql setup might need adjustment for perfect match, but
                // let's check if we can find the offer via API or repo
                // Using repo directly for simplicity of verification logic
                List<CardOffer> matchesBefore = cardOfferRepository.findMatchingOffers(1L, List.of(2L), false, false,
                                true);
                assertFalse(matchesBefore.isEmpty(), "Should have matches before reservation");

                // Reserve the cards (simulate accept)
                ExchangeRequest request = exchangeRequestRepository.findAll().get(0);
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify match is GONE
                List<CardOffer> matchesAfter = cardOfferRepository.findMatchingOffers(1L, List.of(2L), false, false,
                                true);
                // Specifically check for the reserved stickers
                boolean containsReserved = matchesAfter.stream()
                                .anyMatch(o -> o.getStickerId() == 6L || o.getStickerId() == 1L);
                assertFalse(containsReserved, "Reserved cards should not appear in matching offers");
        }

        @Test
        void shouldExposeIsReservedInResponse() throws Exception {
                ExchangeRequest request = exchangeRequestRepository.findAll().get(0);

                // Accept request
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Check Offerer's offers
                mockMvc.perform(get("/api/card-offers")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[?(@.stickerId == 6)].isReserved").value(true));

                // Check Requester's searches
                mockMvc.perform(get("/api/card-searches")
                                .header("Authorization", requesterToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[?(@.stickerId == 6)].isReserved").value(true));
        }

        @Test
        void shouldExcludeReservedCardsFromNativeMatchmakingQueries() {
                // Use User 6 (No Match) to avoid interference from existing test data
                // User 6 offers Sticker 15 (Freebie)
                // User 7 needs Sticker 15
                CardOffer freebieOffer = CardOffer.builder()
                                .userId(6L)
                                .sticker(com.daspawnw.sammelalbum.model.Sticker.builder().id(15L).build())
                                .stickerId(15L)
                                .offerFreebie(true)
                                .isReserved(false)
                                .build();
                cardOfferRepository.save(freebieOffer);

                CardSearch freebieSearch = CardSearch.builder()
                                .userId(7L)
                                .stickerId(15L)
                                .isReserved(false)
                                .build();
                cardSearchRepository.save(freebieSearch);
                entityManager.flush();

                // Verify match exists (User 7 looking for freebies)
                var freebieMatches = cardOfferRepository.findFreebieMatches(7L,
                                org.springframework.data.domain.Pageable.unpaged());
                assertTrue(freebieMatches.stream().anyMatch(m -> m.getUserId().equals(6L)),
                                "Should find freebie match before reservation");

                // Reserve the offer
                freebieOffer.setIsReserved(true);
                cardOfferRepository.save(freebieOffer);
                entityManager.flush();

                // Verify match is gone
                freebieMatches = cardOfferRepository.findFreebieMatches(7L,
                                org.springframework.data.domain.Pageable.unpaged());
                assertFalse(freebieMatches.stream().anyMatch(m -> m.getUserId().equals(6L)),
                                "Should NOT find freebie match after offer reservation");

                // Reset offer, reserve search
                freebieOffer.setIsReserved(false);
                cardOfferRepository.save(freebieOffer);
                freebieSearch.setIsReserved(true);
                cardSearchRepository.save(freebieSearch);
                entityManager.flush();

                // Verify match is gone
                freebieMatches = cardOfferRepository.findFreebieMatches(7L,
                                org.springframework.data.domain.Pageable.unpaged());
                assertFalse(freebieMatches.stream().anyMatch(m -> m.getUserId().equals(6L)),
                                "Should NOT find freebie match after search reservation");
        }

        @Test
        void shouldExcludeReservedCardsFromExchangeMatchmakingQuery() {
                // User 1 offers 6 (EXCHANGE), Needs 1
                // User 2 offers 1 (EXCHANGE), Needs 6
                // (This setup is already done in setUp() but let's verify the native query)

                // Verify match exists
                var exchangeMatches = cardOfferRepository.findExchangeMatches(1L,
                                org.springframework.data.domain.Pageable.unpaged());
                assertTrue(exchangeMatches.stream().anyMatch(m -> m.getUserId().equals(2L)),
                                "Should find exchange match before reservation");

                // Reserve User 1's offer (Sticker 6)
                CardOffer offer6 = cardOfferRepository.findByUserIdAndStickerIdIn(1L, List.of(6L)).get(0);
                offer6.setIsReserved(true);
                cardOfferRepository.save(offer6);

                // Verify match is gone (User 1's perspective)
                exchangeMatches = cardOfferRepository.findExchangeMatches(1L,
                                org.springframework.data.domain.Pageable.unpaged());
                assertFalse(exchangeMatches.stream().anyMatch(m -> m.getUserId().equals(2L)),
                                "Should NOT find exchange match after my offer reservation");

                // Reset
                offer6.setIsReserved(false);
                cardOfferRepository.save(offer6);

                // Reserve User 2's offer (Sticker 1)
                CardOffer offer1 = cardOfferRepository.findByUserIdAndStickerIdIn(2L, List.of(1L)).get(0);
                offer1.setIsReserved(true);
                cardOfferRepository.save(offer1);

                // Verify match is gone
                exchangeMatches = cardOfferRepository.findExchangeMatches(1L,
                                org.springframework.data.domain.Pageable.unpaged());
                assertFalse(exchangeMatches.stream().anyMatch(m -> m.getUserId().equals(2L)),
                                "Should NOT find exchange match after partner offer reservation");
        }
}
