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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExchangeCloseIntegrationTest {

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
        private String unauthorizedToken;
        private Long requesterId;
        private Long offererId;
        private Long unauthorizedUserId;
        private Long sticker1Id;
        private Long sticker2Id;

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
                // Create Stickers
                Sticker s1 = stickerRepository.save(Sticker.builder().id(1L).name("Sticker 1").build());
                Sticker s2 = stickerRepository.save(Sticker.builder().id(2L).name("Sticker 2").build());
                sticker1Id = s1.getId();
                sticker2Id = s2.getId();

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

                User unauthorized = userRepository.save(User.builder()
                                .firstname("Unauthorized").lastname("User").mail("unauthorized@example.com").build());
                credentialsRepository.save(Credentials.builder()
                                .user(unauthorized).username("unauthorized")
                                .passwordHash(passwordEncoder.encode("password"))
                                .build());
                unauthorizedUserId = unauthorized.getId();

                // Generate Tokens
                com.daspawnw.sammelalbum.security.CustomUserDetails requesterDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                                "requester", "password", java.util.Collections.emptyList(), requesterId);
                requesterToken = "Bearer " + jwtService.generateToken(requesterDetails, requesterId);

                com.daspawnw.sammelalbum.security.CustomUserDetails offererDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                                "offerer", "password", java.util.Collections.emptyList(), offererId);
                offererToken = "Bearer " + jwtService.generateToken(offererDetails, offererId);

                com.daspawnw.sammelalbum.security.CustomUserDetails unauthorizedDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                                "unauthorized", "password", java.util.Collections.emptyList(), unauthorizedUserId);
                unauthorizedToken = "Bearer " + jwtService.generateToken(unauthorizedDetails, unauthorizedUserId);
        }

        @Test
        void closeRequest_Unauthorized_ShouldReturn403() throws Exception {
                // Setup: Create a PAYED exchange request
                cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerPayed(true)
                                .isReserved(true)
                                .build());
                cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .build());

                // Attempt to close with unauthorized user
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", unauthorizedToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void closeRequest_WrongStatus_ShouldReturn400() throws Exception {
                // Setup: Create a request in INITIAL status
                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.INITIAL)
                                .build());

                // Attempt to close
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", requesterToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void closeRequest_RequesterClose_PAYED_ShouldDeleteCardSearch() throws Exception {
                // Setup: PAYED exchange
                CardOffer offererOffer = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerPayed(true)
                                .isReserved(true)
                                .build());
                CardSearch requesterSearch = cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(offererOffer.getId())
                                .requesterCardSearchId(requesterSearch.getId())
                                .build());

                // Requester closes
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", requesterToken))
                                .andExpect(status().isOk());

                // Verify: Requester's CardSearch is deleted
                assertFalse(cardSearchRepository.findById(requesterSearch.getId()).isPresent(),
                                "Requester's CardSearch should be deleted");

                // Verify: Offerer's CardOffer still exists (they haven't closed yet)
                assertTrue(cardOfferRepository.findById(offererOffer.getId()).isPresent(),
                                "Offerer's CardOffer should still exist");

                // Verify: Request is marked as requesterClosed but NOT completed
                ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
                assertTrue(updated.getRequesterClosed(), "requesterClosed should be true");
                assertFalse(updated.getOffererClosed(), "offererClosed should be false");
                assertEquals(ExchangeStatus.EXCHANGE_INTERREST, updated.getStatus(),
                                "Status should still be EXCHANGE_INTERREST");
        }

        @Test
        void closeRequest_OffererClose_PAYED_ShouldDeleteCardOffer() throws Exception {
                // Setup: PAYED exchange
                CardOffer offererOffer = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerPayed(true)
                                .isReserved(true)
                                .build());
                CardSearch requesterSearch = cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(offererOffer.getId())
                                .requesterCardSearchId(requesterSearch.getId())
                                .build());

                // Offerer closes
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: Offerer's CardOffer is deleted
                assertFalse(cardOfferRepository.findById(offererOffer.getId()).isPresent(),
                                "Offerer's CardOffer should be deleted");

                // Verify: Requester's CardSearch still exists
                assertTrue(cardSearchRepository.findById(requesterSearch.getId()).isPresent(),
                                "Requester's CardSearch should still exist");

                // Verify: Request is marked as offererClosed but NOT completed
                ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
                assertFalse(updated.getRequesterClosed(), "requesterClosed should be false");
                assertTrue(updated.getOffererClosed(), "offererClosed should be true");
                assertEquals(ExchangeStatus.EXCHANGE_INTERREST, updated.getStatus(),
                                "Status should still be EXCHANGE_INTERREST");
        }

        @Test
        void closeRequest_BothClose_PAYED_ShouldComplete() throws Exception {
                // Setup: PAYED exchange
                CardOffer offererOffer = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerPayed(true)
                                .isReserved(true)
                                .build());
                CardSearch requesterSearch = cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(offererOffer.getId())
                                .requesterCardSearchId(requesterSearch.getId())
                                .build());

                // Requester closes first
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", requesterToken))
                                .andExpect(status().isOk());

                // Offerer closes second
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: Both items are deleted
                assertFalse(cardSearchRepository.findById(requesterSearch.getId()).isPresent());
                assertFalse(cardOfferRepository.findById(offererOffer.getId()).isPresent());

                // Verify: Status is EXCHANGE_COMPLETED
                ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
                assertTrue(updated.getRequesterClosed());
                assertTrue(updated.getOffererClosed());
                assertEquals(ExchangeStatus.EXCHANGE_COMPLETED, updated.getStatus());
        }

        @Test
        void closeRequest_Idempotent_ShouldNotDeleteTwice() throws Exception {
                // Setup: PAYED exchange with multiple copies
                CardOffer offererOffer1 = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerPayed(true)
                                .isReserved(true)
                                .build());
                CardOffer offererOffer2 = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerPayed(true)
                                .isReserved(false)
                                .build());
                CardSearch requesterSearch = cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());

                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(offererOffer1.getId())
                                .requesterCardSearchId(requesterSearch.getId())
                                .build());

                // Offerer closes first time
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: Only reserved offer is deleted
                assertFalse(cardOfferRepository.findById(offererOffer1.getId()).isPresent());
                assertTrue(cardOfferRepository.findById(offererOffer2.getId()).isPresent());

                // Offerer closes second time (idempotent)
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: Second offer is NOT deleted
                assertTrue(cardOfferRepository.findById(offererOffer2.getId()).isPresent(),
                                "Unreserved offer should not be deleted on second close");
        }

        @Test
        void closeRequest_EXCHANGE_BothClose_ShouldDeleteAllFourItems() throws Exception {
                // Setup: EXCHANGE type
                CardOffer offererOffer = cardOfferRepository.save(CardOffer.builder()
                                .userId(offererId)
                                .stickerId(sticker1Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());
                CardSearch requesterSearch1 = cardSearchRepository.save(CardSearch.builder()
                                .userId(requesterId)
                                .stickerId(sticker1Id)
                                .isReserved(true)
                                .build());
                CardOffer requesterOffer = cardOfferRepository.save(CardOffer.builder()
                                .userId(requesterId)
                                .stickerId(sticker2Id)
                                .offerExchange(true)
                                .isReserved(true)
                                .build());
                CardSearch offererSearch = cardSearchRepository.save(CardSearch.builder()
                                .userId(offererId)
                                .stickerId(sticker2Id)
                                .isReserved(true)
                                .build());

                ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(sticker1Id)
                                .offeredStickerId(sticker2Id)
                                .exchangeType(ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(offererOffer.getId())
                                .requesterCardSearchId(requesterSearch1.getId())
                                .requesterCardOfferId(requesterOffer.getId())
                                .offererCardSearchId(offererSearch.getId())
                                .build());

                // Requester closes
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", requesterToken))
                                .andExpect(status().isOk());

                // Verify: Requester's items deleted
                assertFalse(cardSearchRepository.findById(requesterSearch1.getId()).isPresent());
                assertFalse(cardOfferRepository.findById(requesterOffer.getId()).isPresent());

                // Offerer closes
                mockMvc.perform(put("/api/exchanges/" + request.getId() + "/close")
                                .header("Authorization", offererToken))
                                .andExpect(status().isOk());

                // Verify: All four items deleted
                assertFalse(cardOfferRepository.findById(offererOffer.getId()).isPresent());
                assertFalse(cardSearchRepository.findById(offererSearch.getId()).isPresent());

                // Verify: Status is EXCHANGE_COMPLETED
                ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
                assertEquals(ExchangeStatus.EXCHANGE_COMPLETED, updated.getStatus());
        }
}
