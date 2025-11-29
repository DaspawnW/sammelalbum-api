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
class ExchangeDeclineIntegrationTest {

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
    private Long sticker2Id;

    @BeforeTransaction
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
                .user(requester).username("requester").passwordHash(passwordEncoder.encode("password")).build());
        requesterId = requester.getId();

        User offerer = userRepository.save(User.builder()
                .firstname("Offerer").lastname("User").mail("offerer@example.com").build());
        credentialsRepository.save(Credentials.builder()
                .user(offerer).username("offerer").passwordHash(passwordEncoder.encode("password")).build());
        offererId = offerer.getId();

        // Generate Tokens
        com.daspawnw.sammelalbum.security.CustomUserDetails requesterDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                "requester", "password", java.util.Collections.emptyList(), requesterId);
        requesterToken = "Bearer " + jwtService.generateToken(requesterDetails, requesterId);

        com.daspawnw.sammelalbum.security.CustomUserDetails offererDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                "offerer", "password", java.util.Collections.emptyList(), offererId);
        offererToken = "Bearer " + jwtService.generateToken(offererDetails, offererId);
    }

    private void ensureEntities(Long requesterId, Long offererId, Long requestedStickerId, Long offeredStickerId) {
        // Offerer has requested sticker (Exchange=true)
        if (cardOfferRepository.findByUserIdAndStickerIdIn(offererId, List.of(requestedStickerId)).isEmpty()) {
            cardOfferRepository.save(CardOffer.builder()
                    .userId(offererId)
                    .stickerId(requestedStickerId)
                    .offerExchange(true)
                    .build());
        }

        // Requester wants requested sticker
        if (cardSearchRepository.findByUserIdAndStickerIdIn(requesterId, List.of(requestedStickerId)).isEmpty()) {
            cardSearchRepository.save(CardSearch.builder()
                    .userId(requesterId)
                    .stickerId(requestedStickerId)
                    .build());
        }

        if (offeredStickerId != null) {
            // Requester has offered sticker (Exchange=true)
            if (cardOfferRepository.findByUserIdAndStickerIdIn(requesterId, List.of(offeredStickerId)).isEmpty()) {
                cardOfferRepository.save(CardOffer.builder()
                        .userId(requesterId)
                        .stickerId(offeredStickerId)
                        .offerExchange(true)
                        .build());
            }

            // Offerer wants offered sticker
            if (cardSearchRepository.findByUserIdAndStickerIdIn(offererId, List.of(offeredStickerId)).isEmpty()) {
                cardSearchRepository.save(CardSearch.builder()
                        .userId(offererId)
                        .stickerId(offeredStickerId)
                        .build());
            }
        }
    }

    @Test
    void declineRequest_InitialStatus_ShouldSetToCanceled() throws Exception {
        ensureEntities(requesterId, offererId, sticker1Id, sticker2Id);
        ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(sticker1Id)
                .offeredStickerId(sticker2Id)
                .exchangeType(ExchangeType.EXCHANGE)
                .status(ExchangeStatus.INITIAL)
                .build());

        mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                .header("Authorization", offererToken))
                .andExpect(status().isOk());

        ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
        assertEquals(ExchangeStatus.EXCHANGE_CANCELED, updated.getStatus());
    }

    @Test
    void declineRequest_MailSendStatus_ShouldSetToCanceled() throws Exception {
        ensureEntities(requesterId, offererId, sticker1Id, sticker2Id);
        ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(sticker1Id)
                .offeredStickerId(sticker2Id)
                .exchangeType(ExchangeType.EXCHANGE)
                .status(ExchangeStatus.MAIL_SEND)
                .build());

        mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                .header("Authorization", offererToken))
                .andExpect(status().isOk());

        ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
        assertEquals(ExchangeStatus.EXCHANGE_CANCELED, updated.getStatus());
    }

    @Test
    void declineRequest_ExchangeInterestStatus_ShouldRevertReservations_ExchangeType() throws Exception {
        ensureEntities(requesterId, offererId, sticker1Id, sticker2Id);

        // Setup reserved items
        CardOffer offererOffer = cardOfferRepository.findByUserIdAndStickerIdIn(offererId, List.of(sticker1Id)).get(0);
        offererOffer.setIsReserved(true);
        cardOfferRepository.save(offererOffer);

        CardSearch requesterSearch = cardSearchRepository.findByUserIdAndStickerIdIn(requesterId, List.of(sticker1Id))
                .get(0);
        requesterSearch.setIsReserved(true);
        cardSearchRepository.save(requesterSearch);

        CardOffer requesterOffer = cardOfferRepository.findByUserIdAndStickerIdIn(requesterId, List.of(sticker2Id))
                .get(0);
        requesterOffer.setIsReserved(true);
        cardOfferRepository.save(requesterOffer);

        CardSearch offererSearch = cardSearchRepository.findByUserIdAndStickerIdIn(offererId, List.of(sticker2Id))
                .get(0);
        offererSearch.setIsReserved(true);
        cardSearchRepository.save(offererSearch);

        ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(sticker1Id)
                .offeredStickerId(sticker2Id)
                .exchangeType(ExchangeType.EXCHANGE)
                .status(ExchangeStatus.EXCHANGE_INTERREST)
                .build());

        mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                .header("Authorization", offererToken))
                .andExpect(status().isOk());

        ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
        assertEquals(ExchangeStatus.EXCHANGE_CANCELED, updated.getStatus());

        // Verify reservations reverted
        assertFalse(cardOfferRepository.findById(offererOffer.getId()).get().getIsReserved());
        assertFalse(cardSearchRepository.findById(requesterSearch.getId()).get().getIsReserved());
        assertFalse(cardOfferRepository.findById(requesterOffer.getId()).get().getIsReserved());
        assertFalse(cardSearchRepository.findById(offererSearch.getId()).get().getIsReserved());
    }

    @Test
    void declineRequest_ExchangeInterestStatus_ShouldRevertReservations_PayedType() throws Exception {
        ensureEntities(requesterId, offererId, sticker1Id, null);

        // Setup reserved items (only one way for Payed)
        CardOffer offererOffer = cardOfferRepository.findByUserIdAndStickerIdIn(offererId, List.of(sticker1Id)).get(0);
        offererOffer.setIsReserved(true);
        cardOfferRepository.save(offererOffer);

        CardSearch requesterSearch = cardSearchRepository.findByUserIdAndStickerIdIn(requesterId, List.of(sticker1Id))
                .get(0);
        requesterSearch.setIsReserved(true);
        cardSearchRepository.save(requesterSearch);

        ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(sticker1Id)
                .exchangeType(ExchangeType.PAYED)
                .status(ExchangeStatus.EXCHANGE_INTERREST)
                .build());

        mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                .header("Authorization", offererToken))
                .andExpect(status().isOk());

        ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
        assertEquals(ExchangeStatus.EXCHANGE_CANCELED, updated.getStatus());

        // Verify reservations reverted
        assertFalse(cardOfferRepository.findById(offererOffer.getId()).get().getIsReserved());
        assertFalse(cardSearchRepository.findById(requesterSearch.getId()).get().getIsReserved());
    }

    @Test
    void declineRequest_RequesterCanDecline() throws Exception {
        ensureEntities(requesterId, offererId, sticker1Id, sticker2Id);
        ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(sticker1Id)
                .offeredStickerId(sticker2Id)
                .exchangeType(ExchangeType.EXCHANGE)
                .status(ExchangeStatus.MAIL_SEND)
                .build());

        mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                .header("Authorization", requesterToken))
                .andExpect(status().isOk());

        ExchangeRequest updated = exchangeRequestRepository.findById(request.getId()).get();
        assertEquals(ExchangeStatus.EXCHANGE_CANCELED, updated.getStatus());
    }

    @Test
    void declineRequest_UnauthorizedUser_ShouldFail() throws Exception {
        ensureEntities(requesterId, offererId, sticker1Id, sticker2Id);
        ExchangeRequest request = exchangeRequestRepository.save(ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(sticker1Id)
                .offeredStickerId(sticker2Id)
                .exchangeType(ExchangeType.EXCHANGE)
                .status(ExchangeStatus.MAIL_SEND)
                .build());

        // Create a third user
        User thirdUser = userRepository.save(User.builder()
                .firstname("Third").lastname("User").mail("third@example.com").build());
        credentialsRepository.save(Credentials.builder()
                .user(thirdUser).username("thirduser").passwordHash(passwordEncoder.encode("password")).build());

        com.daspawnw.sammelalbum.security.CustomUserDetails thirdUserDetails = new com.daspawnw.sammelalbum.security.CustomUserDetails(
                "thirduser", "password", java.util.Collections.emptyList(), thirdUser.getId());
        String thirdUserToken = "Bearer " + jwtService.generateToken(thirdUserDetails, thirdUser.getId());

        mockMvc.perform(put("/api/exchanges/" + request.getId() + "/decline")
                .header("Authorization", thirdUserToken))
                .andExpect(status().isForbidden()); // Or 403/500 depending on exception handling, SecurityException
                                                    // usually 403 or 500
    }
}
