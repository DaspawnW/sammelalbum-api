package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

        @Mock
        private ExchangeRequestRepository exchangeRequestRepository;

        @Mock
        private CardOfferRepository cardOfferRepository;

        @Mock
        private com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;

        @Mock
        private StickerRepository stickerRepository;

        @Mock
        private NotificationService notificationService;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private ExchangeService exchangeService;

        @Test
        void createExchangeRequest_ShouldThrowException_WhenDuplicateExists() {
                // Arrange
                Long requesterId = 1L;
                Long offererId = 2L;
                Long requestedStickerId = 100L;
                Long offeredStickerId = 200L;
                ExchangeType type = ExchangeType.EXCHANGE;

                when(exchangeRequestRepository
                                .existsByRequesterIdAndOffererIdAndRequestedStickerIdAndOfferedStickerIdAndExchangeTypeAndStatusIn(
                                                eq(requesterId), eq(offererId), eq(requestedStickerId),
                                                eq(offeredStickerId), eq(type),
                                                anyList()))
                                .thenReturn(true);

                // Act & Assert
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> exchangeService
                                .createExchangeRequest(requesterId, offererId, requestedStickerId, offeredStickerId,
                                                type));

                assertEquals("An active exchange request already exists for this selection", exception.getMessage());
                verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
        }

        @Test
        void createExchangeRequest_ShouldThrowException_WhenSelfRequest() {
                assertThrows(IllegalArgumentException.class,
                                () -> exchangeService.createExchangeRequest(1L, 1L, 100L, 200L, ExchangeType.EXCHANGE));
        }

        @Test
        void getSentRequests_ShouldReturnStickerNames() {
                // Arrange
                Long requesterId = 1L;
                Long offererId = 2L;
                Long requestedStickerId = 100L;
                Long offeredStickerId = 200L;

                ExchangeRequest request = ExchangeRequest.builder()
                                .id(1L)
                                .requesterId(requesterId)
                                .offererId(offererId)
                                .requestedStickerId(requestedStickerId)
                                .offeredStickerId(offeredStickerId)
                                .exchangeType(ExchangeType.EXCHANGE)
                                .status(ExchangeStatus.INITIAL)
                                .build();

                com.daspawnw.sammelalbum.model.Sticker requestedSticker = new com.daspawnw.sammelalbum.model.Sticker();
                requestedSticker.setId(requestedStickerId);
                requestedSticker.setName("Charizard");

                com.daspawnw.sammelalbum.model.Sticker offeredSticker = new com.daspawnw.sammelalbum.model.Sticker();
                offeredSticker.setId(offeredStickerId);
                offeredSticker.setName("Pikachu");

                when(exchangeRequestRepository.findByRequesterId(requesterId)).thenReturn(List.of(request));
                when(stickerRepository.findAllById(anySet())).thenReturn(List.of(requestedSticker, offeredSticker));

                // Act
                List<com.daspawnw.sammelalbum.dto.ExchangeRequestDto> result = exchangeService
                                .getSentRequests(requesterId);

                // Assert
                assertEquals(1, result.size());
                assertEquals("Charizard", result.get(0).getRequestedStickerName());
                assertEquals("Pikachu", result.get(0).getOfferedStickerName());
        }
}
