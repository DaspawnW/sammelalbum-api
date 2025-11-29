package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.*;
import com.daspawnw.sammelalbum.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExchangeMultipleItemsIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CredentialsRepository credentialsRepository;

        @Autowired
        private StickerRepository stickerRepository;

        @Autowired
        private CardOfferRepository cardOfferRepository;

        @Autowired
        private CardSearchRepository cardSearchRepository;

        @Autowired
        private ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private com.daspawnw.sammelalbum.service.JwtService jwtService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private String requesterToken;
        private String offererToken;
        private Long requesterId;
        private Long offererId;
        private Long sticker1Id;

        @BeforeTransaction
        void cleanup() {
                jdbcTemplate.execute("DELETE FROM shedlock");
                jdbcTemplate.execute("DELETE FROM email_outbox");
                jdbcTemplate.execute("DELETE FROM exchange_requests");
                jdbcTemplate.execute("DELETE FROM card_searches");
                jdbcTemplate.execute("DELETE FROM card_offers");
                jdbcTemplate.execute("DELETE FROM credentials");
                jdbcTemplate.execute("DELETE FROM users");
                jdbcTemplate.execute("DELETE FROM stickers");
        }

        @BeforeEach
        void setup() throws Exception {
                // Create Sticker
                Sticker s1 = stickerRepository.save(Sticker.builder().id(1L).name("Sticker 1").build());
                sticker1Id = s1.getId();

                // Create Users
                User requester = userRepository.save(User.builder()
                                .firstname("Requester").lastname("User").mail("requester@example.com").build());
                credentialsRepository.save(Credentials.builder()
                                .user(requester).username("requester").passwordHash(passwordEncoder.encode("password"))
                                .build());
                requesterId = requester.getId();

                User offerer = userRepository.save(User.builder()
                                .firstname("Offerer").lastname("User").mail("offerer@example.com").build());
                credentialsRepository.save(Credentials.builder()
                                .user(offerer).username("offerer").passwordHash(passwordEncoder.encode("password"))
                                .build());
                offererId = offerer.getId();

                // Generate Tokens
                com.daspawnw.sammelalbum.security.CustomUserDetails requesterDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                                "requester", "password", java.util.Collections.emptyList(), requesterId);
                requesterToken = "Bearer " + jwtService.generateToken(requesterDetails, requesterId);

                com.daspawnw.sammelalbum.security.CustomUserDetails offererDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                                "offerer", "password", java.util.Collections.emptyList(), offererId);
                offererToken = "Bearer " + jwtService.generateToken(offererDetails, offererId);
        }

        @Test
        void acceptRequest_MultipleCopies_ShouldReserveUnreservedCopy() throws Exception {
                // Offerer has 2 copies of Sticker 1
                // Copy 1: Reserved (simulating another active exchange)
                cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());

                // Copy 2: Available
                CardOffer availableOffer = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerExchange(true)
                                .isReserved(false)
                                .build());

                // Requester wants Sticker 1
                cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .build());

                // Create Request (PAYED type for simplicity, only one sticker involved)
                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.MAIL_SEND)
                                .build());

                // Accept Request
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/accept")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: Copy 2 should now be reserved
                CardOffer updatedOffer = cardOfferRepository.findById(availableOffer.getId()).get();
                assertTrue(updatedOffer.getIsReserved(), "The available offer should have been reserved");
        }

        @Test
        void declineRequest_MultipleCopies_ShouldUnreserveReservedCopy() throws Exception {
                // Offerer has 2 copies of Sticker 1
                // Copy 1: Reserved (for this exchange)
                CardOffer reservedOffer1 = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());

                // Copy 2: Reserved (for another exchange)
                CardOffer reservedOffer2 = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());

                // Requester wants Sticker 1 (Reserved)
                cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                // Create Request (PAYED type) in EXCHANGE_INTERREST status
                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .build());

                // Decline Request
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: One of the copies should be unreserved
                // Since we don't link specific offers to requests, unreserving ANY reserved
                // copy is acceptable behavior
                // as long as the total count of reserved items decreases by 1.
                List<CardOffer> offers = cardOfferRepository.findByUserIdAndStickerIdIn(offererId, List.of(sticker1Id));
                long reservedCount = offers.stream().filter(CardOffer::getIsReserved).count();
                assertEquals(1, reservedCount, "Exactly one offer should remain reserved");
        }

        @Test
        void declineRequest_ExchangeType_ShouldUnreserveBothSides() throws Exception {
                // Setup: Exchange where Requester wants Sticker 1 (from Offerer) and Offerer
                // wants Sticker 2 (from Requester)
                Sticker s2 = stickerRepository.save(Sticker.builder().id(2L).name("Sticker 2").build());
                Long sticker2Id = s2.getId();

                // 1. Requested Sticker (Sticker 1)
                // Offerer has Sticker 1 (Reserved)
                cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());
                // Requester wants Sticker 1 (Reserved)
                cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                // 2. Offered Sticker (Sticker 2)
                // Requester has Sticker 2 (Reserved)
                cardOfferRepository.save(CardOffer.builder()
                                .userId(requesterId)
                                .stickerId(sticker2Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());
                // Offerer wants Sticker 2 (Reserved)
                cardSearchRepository.save(CardSearch.builder()
                                .userId(offererId)
                                .stickerId(sticker2Id)
                                .isReserved(true)
                                .build());

                // Create Request (EXCHANGE type) in EXCHANGE_INTERREST status
                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .offeredStickerId(sticker2Id)
                                .exchangeType(ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .build());

                // Decline Request
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: ALL 4 items should be unreserved

                // 1. Requested Sticker Side
                List<CardOffer> offererOffers = cardOfferRepository.findByUserIdAndStickerIdIn(offererId,
                                List.of(sticker1Id));
                assertFalse(offererOffers.get(0).getIsReserved(), "Offerer's offer for Sticker 1 should be unreserved");

                List<CardSearch> requesterSearches = cardSearchRepository.findByUserIdAndStickerIdIn(requesterId,
                                List.of(sticker1Id));
                assertFalse(requesterSearches.get(0).getIsReserved(),
                                "Requester's search for Sticker 1 should be unreserved");

                // 2. Offered Sticker Side
                List<CardOffer> requesterOffers = cardOfferRepository.findByUserIdAndStickerIdIn(requesterId,
                                List.of(sticker2Id));
                assertFalse(requesterOffers.get(0).getIsReserved(),
                                "Requester's offer for Sticker 2 should be unreserved");

                List<CardSearch> offererSearches = cardSearchRepository.findByUserIdAndStickerIdIn(offererId,
                                List.of(sticker2Id));
                assertFalse(offererSearches.get(0).getIsReserved(),
                                "Offerer's search for Sticker 2 should be unreserved");
        }
}
