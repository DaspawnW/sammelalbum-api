package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkCardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferResponse;
import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardOfferServiceTest {

    @Mock
    private CardOfferRepository cardOfferRepository;

    @Mock
    private StickerRepository stickerRepository;

    @Mock
    private ExchangeService exchangeService;

    @InjectMocks
    private CardOfferService cardOfferService;

    @Test
    void addOffer_Success() {
        Long userId = 1L;
        CardOfferRequest request = new CardOfferRequest(100L, true, false, true); // Payed and Exchange
        CardOffer savedOffer = CardOffer.builder()
                .id(1L)
                .userId(userId)
                .stickerId(100L)
                .offerPayed(true)
                .offerFreebie(false)
                .offerExchange(true)
                .build();

        when(stickerRepository.existsById(100L)).thenReturn(true);
        when(cardOfferRepository.save(any(CardOffer.class))).thenReturn(savedOffer);

        CardOfferResponse response = cardOfferService.addOffer(userId, request);

        assertNotNull(response);
        assertEquals(100L, response.getStickerId());
        assertTrue(response.getOfferPayed());
        assertFalse(response.getOfferFreebie());
        assertTrue(response.getOfferExchange());
        verify(stickerRepository).existsById(100L);
        verify(cardOfferRepository).save(any(CardOffer.class));
    }

    @Test
    void addOffer_StickerNotFound() {
        Long userId = 1L;
        CardOfferRequest request = new CardOfferRequest(999L, true, false, false);

        when(stickerRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> cardOfferService.addOffer(userId, request));
        verify(cardOfferRepository, never()).save(any());
    }

    @Test
    void deleteOffer_Success() {
        Long userId = 1L;
        Long offerId = 10L;
        CardOffer offer = CardOffer.builder().id(offerId).userId(userId).build();

        when(cardOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        cardOfferService.deleteOffer(userId, offerId);

        verify(cardOfferRepository).delete(offer);
    }

    @Test
    void deleteOffer_Forbidden() {
        Long userId = 1L;
        Long otherUserId = 2L;
        Long offerId = 10L;
        CardOffer offer = CardOffer.builder().id(offerId).userId(otherUserId).build();

        when(cardOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        assertThrows(SecurityException.class, () -> cardOfferService.deleteOffer(userId, offerId));
        verify(cardOfferRepository, never()).delete(any());
    }

    @Test
    void removeBulkOffers_SmartDelete() {
        Long userId = 1L;
        List<Long> stickerIds = Arrays.asList(100L, 100L); // Delete two instances of 100
        BulkCardOfferRequest request = new BulkCardOfferRequest(stickerIds, null, null, null);

        // User has 3 instances of 100
        CardOffer o1 = CardOffer.builder().id(1L).userId(userId).stickerId(100L).build();
        CardOffer o2 = CardOffer.builder().id(2L).userId(userId).stickerId(100L).build();
        CardOffer o3 = CardOffer.builder().id(3L).userId(userId).stickerId(100L).build();
        List<CardOffer> userOffers = Arrays.asList(o1, o2, o3);

        when(cardOfferRepository.findByUserIdAndStickerIdIn(userId, stickerIds)).thenReturn(userOffers);

        cardOfferService.removeBulkOffers(userId, request);

        verify(cardOfferRepository).deleteAll(anyList());
    }
}
