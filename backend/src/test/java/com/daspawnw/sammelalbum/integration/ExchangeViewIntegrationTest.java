package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.dto.ExchangeRequestDto;
import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
import com.daspawnw.sammelalbum.model.CancellationReason;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.ExchangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class ExchangeViewIntegrationTest {

        @Autowired
        private ExchangeService exchangeService;

        @Autowired
        private ExchangeRequestRepository exchangeRequestRepository;

        @Autowired
        private UserRepository userRepository;

        @Test
        void getSentRequests_ShouldReturnRequests_WithCorrectVisibility() {
                // Requester ID 1 (Alice)
                // Offerer ID 4 (David)

                // Update User 4 (David) to have contact info
                User david = userRepository.findById(4L).orElseThrow();
                david.setContact("david@contact.com");
                userRepository.save(david);

                // 1. Create INITIAL request (No partner info expected)
                ExchangeRequest req1 = ExchangeRequest.builder()
                                .requesterId(1L)
                                .offererId(4L)
                                .requestedStickerId(1L)
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.INITIAL)
                                .build();
                exchangeRequestRepository.save(req1);

                // 2. Create EXCHANGE_INTERREST request (Partner info expected)
                ExchangeRequest req2 = ExchangeRequest.builder()
                                .requesterId(1L)
                                .offererId(4L)
                                .requestedStickerId(2L)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .build();
                exchangeRequestRepository.save(req2);

                List<ExchangeRequestDto> sentRequests = exchangeService.getSentRequests(1L);

                // There might be other requests from match_scenarios.sql, so we filter by ID or
                // check containment
                // But match_scenarios.sql doesn't create ExchangeRequests, only
                // Offers/Searches.
                // So size should be 2.
                assertEquals(2, sentRequests.size());

                // Verify req1 (INITIAL)
                ExchangeRequestDto dto1 = sentRequests.stream().filter(r -> r.getId().equals(req1.getId())).findFirst()
                                .orElseThrow();
                assertNull(dto1.getPartnerFirstname());
                assertNull(dto1.getPartnerLastname());
                assertNull(dto1.getPartnerContact());

                // Verify req2 (EXCHANGE_INTERREST)
                ExchangeRequestDto dto2 = sentRequests.stream().filter(r -> r.getId().equals(req2.getId())).findFirst()
                                .orElseThrow();
                assertNotNull(dto2.getPartnerFirstname());
                assertNotNull(dto2.getPartnerLastname());
                assertNotNull(dto2.getPartnerContact());
                assertEquals("Perfect", dto2.getPartnerFirstname()); // User 4 firstname is "Perfect"
                assertEquals("david@contact.com", dto2.getPartnerContact());
        }

        @Test
        void getReceivedOffers_ShouldReturnOffers_WithCorrectVisibility() {
                // Offerer ID 4 (David)
                // Requester ID 1 (Alice)

                // Update User 1 (Alice) to have contact info
                User alice = userRepository.findById(1L).orElseThrow();
                alice.setContact("alice@contact.com");
                userRepository.save(alice);

                // 1. Create MAIL_SEND request (No partner info expected)
                ExchangeRequest req1 = ExchangeRequest.builder()
                                .requesterId(1L)
                                .offererId(4L)
                                .requestedStickerId(1L)
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.MAIL_SEND)
                                .build();
                exchangeRequestRepository.save(req1);

                // 2. Create EXCHANGE_INTERREST request (Partner info expected)
                ExchangeRequest req2 = ExchangeRequest.builder()
                                .requesterId(1L)
                                .offererId(4L)
                                .requestedStickerId(2L)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .build();
                exchangeRequestRepository.save(req2);

                List<ExchangeRequestDto> receivedOffers = exchangeService.getReceivedOffers(4L);

                assertEquals(2, receivedOffers.size());

                // Verify req1 (MAIL_SEND)
                ExchangeRequestDto dto1 = receivedOffers.stream().filter(r -> r.getId().equals(req1.getId()))
                                .findFirst()
                                .orElseThrow();
                assertNull(dto1.getPartnerFirstname());
                assertNull(dto1.getPartnerLastname());
                assertNull(dto1.getPartnerContact());

                // Verify req2 (EXCHANGE_INTERREST)
                ExchangeRequestDto dto2 = receivedOffers.stream().filter(r -> r.getId().equals(req2.getId()))
                                .findFirst()
                                .orElseThrow();
                assertNotNull(dto2.getPartnerFirstname());
                assertNotNull(dto2.getPartnerLastname());
                assertNotNull(dto2.getPartnerContact());
                assertEquals("Main", dto2.getPartnerFirstname()); // User 1 firstname is "Main"
                assertEquals("alice@contact.com", dto2.getPartnerContact());
        }

        @Test
        void getSentRequests_WithCanceledRequest_ShouldReturnCancellationReason() {
                // Create a canceled request with cancellation reason
                ExchangeRequest req = ExchangeRequest.builder()
                                .requesterId(1L)
                                .offererId(4L)
                                .requestedStickerId(1L)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.EXCHANGE_CANCELED)
                                .cancellationReason(
                                                CancellationReason.REQUESTER_CANCELED)
                                .build();
                exchangeRequestRepository.save(req);

                List<ExchangeRequestDto> sentRequests = exchangeService.getSentRequests(1L);

                ExchangeRequestDto dto = sentRequests.stream().filter(r -> r.getId().equals(req.getId())).findFirst()
                                .orElseThrow();
                assertEquals(CancellationReason.REQUESTER_CANCELED,
                                dto.getCancellationReason());
        }
}
