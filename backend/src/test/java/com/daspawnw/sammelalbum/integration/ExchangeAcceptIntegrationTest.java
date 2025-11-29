package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@org.springframework.transaction.annotation.Transactional
public class ExchangeAcceptIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private EmailOutboxRepository emailOutboxRepository;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private String offererToken;
        private String requesterToken;
        private String otherUserToken;

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
                // emailOutboxRepository.deleteAll(); // Handled by @BeforeTransaction cleanup
                // exchangeRequestRepository.deleteAll(); // Handled by @BeforeTransaction
                // cleanup

                // Ensure credentials exist for users 2 and 3 (missing in match_scenarios.sql)
                // User 1 already has credentials 'mainuser'
                jdbcTemplate.update(
                                "MERGE INTO credentials (user_id, username, password_hash) KEY(user_id) VALUES (2, 'freebie', 'hash')");
                jdbcTemplate.update(
                                "MERGE INTO credentials (user_id, username, password_hash) KEY(user_id) VALUES (3, 'payed', 'hash')");

                // Offerer is user ID 1 (Main User) - Username: mainuser
                offererToken = "Bearer "
                                + jwtService.generateToken(
                                                new org.springframework.security.core.userdetails.User("mainuser",
                                                                "password", java.util.Collections.emptyList()),
                                                1L);
                // Requester is user ID 2 (Freebie King) - Username: freebie
                requesterToken = "Bearer "
                                + jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                                                "freebie", "password",
                                                java.util.Collections.emptyList()), 2L);
                // Other user is user ID 3 (Payed Merchant) - Username: payed
                otherUserToken = "Bearer "
                                + jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                                                "payed", "password",
                                                java.util.Collections.emptyList()), 3L);

                // Create a request
                ExchangeRequest request = ExchangeRequest.builder()
                                .requesterId(2L)
                                .offererId(1L)
                                .requestedStickerId(6L) // Main offers 6
                                .offeredStickerId(1L) // Freebie offers 1
                                .exchangeType(com.daspawnw.sammelalbum.model.ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.MAIL_SEND)
                                .build();
                exchangeRequestRepository.save(request);

                // Ensure CardSearch exists for the requested sticker (User 2 needs 6)
                // Check if it exists (from match_scenarios.sql), if not create it
                // Note: match_scenarios.sql might not have it for User 2 specifically if not
                // set up for this exact match
                // But let's just save it to be sure, avoiding duplicates if possible or just
                // relying on cleanup
                // Actually, we should check repository or just save if we don't care about
                // duplicates (but unique constraint might exist)
                // Let's assume unique constraint on user_id + sticker_id

                // Inject CardSearchRepository
        }

        @Autowired
        private com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.CardOfferRepository cardOfferRepository;

        @BeforeEach // Additional setup to ensure CardSearch/CardOffer state
        void ensureEntities() {
                // User 2 needs 6
                if (cardSearchRepository.findByUserIdAndStickerIdIn(2L, List.of(6L)).isEmpty()) {
                        cardSearchRepository.save(com.daspawnw.sammelalbum.model.CardSearch.builder().userId(2L)
                                        .stickerId(6L).build());
                }

                // User 1 needs 1
                if (cardSearchRepository.findByUserIdAndStickerIdIn(1L, List.of(1L)).isEmpty()) {
                        cardSearchRepository.save(com.daspawnw.sammelalbum.model.CardSearch.builder().userId(1L)
                                        .stickerId(1L).build());
                }

                // Ensure User 2 has Sticker 1 as EXCHANGE offer
                // Check if offer exists, if so update, else create
                List<com.daspawnw.sammelalbum.model.CardOffer> offers = cardOfferRepository
                                .findByUserIdAndStickerIdIn(2L, List.of(1L));
                if (!offers.isEmpty()) {
                        com.daspawnw.sammelalbum.model.CardOffer offer = offers.get(0);
                        offer.setOfferExchange(true);
                        cardOfferRepository.save(offer);
                } else {
                        // Create if not exists (though match_scenarios usually has some data)
                        cardOfferRepository.save(com.daspawnw.sammelalbum.model.CardOffer.builder()
                                        .userId(2L)
                                        .stickerId(1L)
                                        .offerExchange(true)
                                        .sticker(com.daspawnw.sammelalbum.model.Sticker.builder().id(1L).build())
                                        .build());
                }
        }

        @Test
        void shouldAcceptExchangeRequestAndSendEmail() throws Exception {
                // 1. Find the existing request created in setUp
                ExchangeRequest request = exchangeRequestRepository.findAll().get(0);

                // 2. Perform Accept Request as Offerer
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // 3. Verify Status Update
                ExchangeRequest updatedRequest = exchangeRequestRepository.findById(request.getId()).orElseThrow();
                assertEquals(ExchangeStatus.EXCHANGE_INTERREST, updatedRequest.getStatus());

                // 4. Verify Email Notification to Requester (User 2)
                List<EmailOutbox> emails = emailOutboxRepository.findAll();
                assertEquals(1, emails.size());
                EmailOutbox email = emails.get(0);
                assertEquals("freebie@example.com", email.getRecipientEmail()); // Requester's email
                assertEquals(EmailStatus.PENDING, email.getStatus());
                assertTrue(email.getBody().contains("Deine Tauschanfrage wurde akzeptiert"));
                assertTrue(email.getBody().contains("Kontaktinformationen"));
        }

        @Test
        void shouldForbidAcceptByWrongUser() throws Exception {
                ExchangeRequest request = exchangeRequestRepository.findAll().get(0);

                // Try to accept as Other User (ID 3)
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", otherUserToken))
                                .andExpect(status().isForbidden());

                // Try to accept as Requester (ID 2) - also forbidden, only offerer can accept
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", requesterToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldFailIfRequestNotFound() throws Exception {
                mockMvc.perform(put("/api/exchanges/9999/accept")
                                .header("Authorization", offererToken))
                                .andExpect(status().isBadRequest());
        }
}
