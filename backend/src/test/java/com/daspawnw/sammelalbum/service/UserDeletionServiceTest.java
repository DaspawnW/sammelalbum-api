package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.model.*;
import com.daspawnw.sammelalbum.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserDeletionService.
 * Tests cover various scenarios including exchange closure, cascade deletion,
 * and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {

        @Mock
        private ExchangeRequestRepository exchangeRequestRepository;

        @Mock
        private CardOfferRepository cardOfferRepository;

        @Mock
        private CardSearchRepository cardSearchRepository;

        @Mock
        private CredentialsRepository credentialsRepository;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private UserDeletionService userDeletionService;

        private User testUser;
        private Credentials testCredentials;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .firstname("Test")
                                .lastname("User")
                                .mail("test@example.com")
                                .build();

                testCredentials = Credentials.builder()
                                .id(1L)
                                .username("testuser")
                                .passwordHash("hash")
                                .user(testUser)
                                .build();
        }

        @Test
        void testDeleteUser_WithNoData_ShouldDeleteSuccessfully() {
                // Given
                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(Collections.emptyList());
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(exchangeRequestRepository).findByRequesterId(1L);
                verify(exchangeRequestRepository).findByOffererId(1L);
                verify(cardOfferRepository).findAllByUserId(1L);
                verify(cardSearchRepository).findAllByUserId(1L);
                verify(credentialsRepository).delete(testCredentials);
        }

        @Test
        void testDeleteUser_WithUnreservedItemsOnly_ShouldDeleteAllItems() {
                // Given
                CardOffer offer1 = CardOffer.builder().id(1L).userId(1L).stickerId(10L).isReserved(false).build();
                CardOffer offer2 = CardOffer.builder().id(2L).userId(1L).stickerId(11L).isReserved(false).build();
                CardSearch search1 = CardSearch.builder().id(1L).userId(1L).stickerId(20L).isReserved(false).build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(Collections.emptyList());
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Arrays.asList(offer1, offer2));
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(List.of(search1));
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(cardOfferRepository).deleteAll(Arrays.asList(offer1, offer2));
                verify(cardSearchRepository).deleteAll(List.of(search1));
                verify(credentialsRepository).delete(testCredentials);
        }

        @Test
        void testDeleteUser_AsRequesterInExchangeInterest_ShouldCloseExchangeAndUnreservePartnerItems() {
                // Given: User is requester in EXCHANGE_INTERREST exchange
                CardOffer offererOffer = CardOffer.builder().id(10L).userId(2L).stickerId(100L).isReserved(true)
                                .build();
                CardSearch offererSearch = CardSearch.builder().id(20L).userId(2L).stickerId(101L).isReserved(true)
                                .build();

                ExchangeRequest exchange = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(1L)
                                .offererId(2L)
                                .requestedStickerId(100L)
                                .offeredStickerId(101L)
                                .exchangeType(ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(10L)
                                .offererCardSearchId(20L)
                                .requesterCardSearchId(30L)
                                .requesterCardOfferId(31L)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(List.of(exchange));
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findById(10L)).thenReturn(Optional.of(offererOffer));
                when(cardSearchRepository.findById(20L)).thenReturn(Optional.of(offererSearch));
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(exchangeRequestRepository).delete(exchange);
                verify(cardOfferRepository).save(argThat(offer -> offer.getId().equals(10L) && !offer.getIsReserved()));
                verify(cardSearchRepository)
                                .save(argThat(search -> search.getId().equals(20L) && !search.getIsReserved()));
        }

        @Test
        void testDeleteUser_AsOffererInExchangeInterest_ShouldCloseExchangeAndUnreservePartnerItems() {
                // Given: User is offerer in EXCHANGE_INTERREST exchange
                CardSearch requesterSearch = CardSearch.builder().id(30L).userId(2L).stickerId(100L).isReserved(true)
                                .build();
                CardOffer requesterOffer = CardOffer.builder().id(31L).userId(2L).stickerId(101L).isReserved(true)
                                .build();

                ExchangeRequest exchange = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(2L)
                                .offererId(1L)
                                .requestedStickerId(100L)
                                .offeredStickerId(101L)
                                .exchangeType(ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(10L)
                                .offererCardSearchId(20L)
                                .requesterCardSearchId(30L)
                                .requesterCardOfferId(31L)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(Collections.emptyList());
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(List.of(exchange));
                when(cardSearchRepository.findById(30L)).thenReturn(Optional.of(requesterSearch));
                when(cardOfferRepository.findById(31L)).thenReturn(Optional.of(requesterOffer));
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(exchangeRequestRepository).delete(exchange);
                verify(cardSearchRepository)
                                .save(argThat(search -> search.getId().equals(30L) && !search.getIsReserved()));
                verify(cardOfferRepository).save(argThat(offer -> offer.getId().equals(31L) && !offer.getIsReserved()));
        }

        @Test
        void testDeleteUser_WithFreebieExchange_ShouldCloseAndUnreserveCorrectly() {
                // Given: User is requester in FREEBIE exchange (no offered sticker)
                CardOffer offererOffer = CardOffer.builder().id(10L).userId(2L).stickerId(100L).isReserved(true)
                                .build();

                ExchangeRequest exchange = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(1L)
                                .offererId(2L)
                                .requestedStickerId(100L)
                                .offeredStickerId(null)
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.EXCHANGE_INTERREST)
                                .offererCardOfferId(10L)
                                .requesterCardSearchId(30L)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(List.of(exchange));
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findById(10L)).thenReturn(Optional.of(offererOffer));
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(exchangeRequestRepository).delete(exchange);
                verify(cardOfferRepository).save(argThat(offer -> offer.getId().equals(10L) && !offer.getIsReserved()));
                // Should not try to unreserve offerer's search (doesn't exist for FREEBIE)
                verify(cardSearchRepository, never()).save(any());
        }

        @Test
        void testDeleteUser_WithCompletedExchange_ShouldNotCloseAgain() {
                // Given: User has a completed exchange
                ExchangeRequest completedExchange = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(1L)
                                .offererId(2L)
                                .requestedStickerId(100L)
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.EXCHANGE_COMPLETED)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(List.of(completedExchange));
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                // Should still delete the completed exchange
                verify(exchangeRequestRepository).delete(completedExchange);
        }

        @Test
        void testDeleteUser_WithCanceledExchange_ShouldNotCloseAgain() {
                // Given: User has a canceled exchange
                ExchangeRequest canceledExchange = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(1L)
                                .offererId(2L)
                                .requestedStickerId(100L)
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.EXCHANGE_CANCELED)
                                .cancellationReason(CancellationReason.REQUESTER_CANCELED)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(List.of(canceledExchange));
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                // Should still delete the canceled exchange
                verify(exchangeRequestRepository).delete(canceledExchange);
        }

        @Test
        void testDeleteUser_InBothRoles_ShouldHandleAllExchanges() {
                // Given: User is both requester and offerer in different exchanges
                ExchangeRequest asRequester = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(1L)
                                .offererId(2L)
                                .requestedStickerId(100L)
                                .exchangeType(ExchangeType.FREEBIE)
                                .status(ExchangeStatus.MAIL_SEND)
                                .build();

                ExchangeRequest asOfferer = ExchangeRequest.builder()
                                .id(2L)
                                .requesterId(3L)
                                .offererId(1L)
                                .requestedStickerId(200L)
                                .exchangeType(ExchangeType.PAYED)
                                .status(ExchangeStatus.INITIAL)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(List.of(asRequester));
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(List.of(asOfferer));
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(exchangeRequestRepository, times(2)).delete(any(ExchangeRequest.class));
        }

        @Test
        void testDeleteUser_WithMixedReservedAndUnreservedItems_ShouldDeleteAll() {
                // Given: User has both reserved and unreserved items
                CardOffer reservedOffer = CardOffer.builder().id(1L).userId(1L).stickerId(10L).isReserved(true).build();
                CardOffer unreservedOffer = CardOffer.builder().id(2L).userId(1L).stickerId(11L).isReserved(false)
                                .build();
                CardSearch reservedSearch = CardSearch.builder().id(3L).userId(1L).stickerId(20L).isReserved(true)
                                .build();
                CardSearch unreservedSearch = CardSearch.builder().id(4L).userId(1L).stickerId(21L).isReserved(false)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L)).thenReturn(Collections.emptyList());
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Arrays.asList(reservedOffer, unreservedOffer));
                when(cardSearchRepository.findAllByUserId(1L))
                                .thenReturn(Arrays.asList(reservedSearch, unreservedSearch));
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(cardOfferRepository).deleteAll(argThat(offers -> {
                        List<CardOffer> offerList = new java.util.ArrayList<>();
                        offers.forEach(offerList::add);
                        return offerList.size() == 2 &&
                                        offerList.contains(reservedOffer) &&
                                        offerList.contains(unreservedOffer);
                }));
                verify(cardSearchRepository).deleteAll(argThat(searches -> {
                        List<CardSearch> searchList = new java.util.ArrayList<>();
                        searches.forEach(searchList::add);
                        return searchList.size() == 2 &&
                                        searchList.contains(reservedSearch) &&
                                        searchList.contains(unreservedSearch);
                }));
        }

        @Test
        void testDeleteUser_UserNotFound_ShouldReturnEarly() {
                // Given
                when(userRepository.existsById(1L)).thenReturn(false);

                // When
                userDeletionService.deleteUser(1L);

                // Then
                verify(exchangeRequestRepository, never()).findByRequesterId(any());
                verify(exchangeRequestRepository, never()).findByOffererId(any());
                verify(cardOfferRepository, never()).findAllByUserId(any());
                verify(cardSearchRepository, never()).findAllByUserId(any());
                verify(credentialsRepository, never()).delete(any());
        }

        @Test
        void testDeleteUser_WithMultipleExchangesInDifferentStatuses_ShouldOnlyCloseActiveOnes() {
                // Given
                ExchangeRequest initial = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(1L)
                                .offererId(2L)
                                .status(ExchangeStatus.INITIAL)
                                .exchangeType(ExchangeType.FREEBIE)
                                .build();

                ExchangeRequest mailSend = ExchangeRequest.builder()
                                .id(2L)
                                .requesterId(1L)
                                .offererId(3L)
                                .status(ExchangeStatus.MAIL_SEND)
                                .exchangeType(ExchangeType.PAYED)
                                .build();

                ExchangeRequest completed = ExchangeRequest.builder()
                                .id(3L)
                                .requesterId(1L)
                                .offererId(4L)
                                .status(ExchangeStatus.EXCHANGE_COMPLETED)
                                .exchangeType(ExchangeType.EXCHANGE)
                                .build();

                ExchangeRequest canceled = ExchangeRequest.builder()
                                .id(4L)
                                .requesterId(1L)
                                .offererId(5L)
                                .status(ExchangeStatus.EXCHANGE_CANCELED)
                                .exchangeType(ExchangeType.FREEBIE)
                                .build();

                when(userRepository.existsById(1L)).thenReturn(true);
                when(exchangeRequestRepository.findByRequesterId(1L))
                                .thenReturn(Arrays.asList(initial, mailSend, completed, canceled));
                when(exchangeRequestRepository.findByOffererId(1L)).thenReturn(Collections.emptyList());
                when(cardOfferRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(cardSearchRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
                when(credentialsRepository.findAll()).thenReturn(List.of(testCredentials));

                // When
                userDeletionService.deleteUser(1L);

                // Then
                // Should delete all 4 exchanges (INITIAL, MAIL_SEND, COMPLETED, and CANCELED)
                verify(exchangeRequestRepository, times(4)).delete(any(ExchangeRequest.class));
        }
}
