package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.MatchDtos.MatchResponse;
import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
import com.daspawnw.sammelalbum.model.Sticker;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.repository.MatchProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceFilteringTest {

        @Mock
        private CardOfferRepository cardOfferRepository;

        @Mock
        private CardSearchRepository cardSearchRepository;

        @Mock
        private ExchangeRequestRepository exchangeRequestRepository;

        @InjectMocks
        private MatchService matchService;

        @Test
        void getFreebieMatches_ShouldFilterOutActiveRequests() {
                // Arrange
                Long userId = 1L;
                Long partnerId = 2L;
                Long stickerId = 100L;

                // Mock MatchProjection
                MatchProjection projection = mock(MatchProjection.class);
                when(projection.getUserId()).thenReturn(partnerId);
                when(projection.getMatchCount()).thenReturn(1L);
                Page<MatchProjection> matches = new PageImpl<>(List.of(projection));

                when(cardOfferRepository.findFreebieMatches(eq(userId), any(Pageable.class))).thenReturn(matches);

                // Mock CardOffer (Partner has sticker 100)
                CardOffer offer = new CardOffer();
                offer.setUserId(partnerId);
                Sticker sticker = new Sticker();
                sticker.setId(stickerId);
                sticker.setName("Sticker 100");
                offer.setSticker(sticker);

                when(cardOfferRepository.findMatchingOffers(eq(userId), anyList(), eq(true), eq(false), eq(false)))
                                .thenReturn(List.of(offer));

                // Mock Active Request for sticker 100
                ExchangeRequest activeRequest = new ExchangeRequest();
                activeRequest.setRequesterId(userId);
                activeRequest.setOffererId(partnerId);
                activeRequest.setRequestedStickerId(stickerId);
                activeRequest.setExchangeType(ExchangeType.FREEBIE);
                activeRequest.setStatus(ExchangeStatus.INITIAL);

                when(exchangeRequestRepository.findByRequesterIdAndOffererIdIn(eq(userId), anyList()))
                                .thenReturn(List.of(activeRequest));

                // Act
                Page<MatchResponse> result = matchService.getFreebieMatches(userId, Pageable.unpaged());

                // Assert
                assertTrue(result.getContent().isEmpty(),
                                "Match should be removed because all requested items were filtered out");
        }
}
