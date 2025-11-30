package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkCardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkUpdateOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferResponse;
import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardOfferService {

    private final CardOfferRepository cardOfferRepository;
    private final StickerRepository stickerRepository;
    private final ExchangeService exchangeService;

    public CardOfferService(CardOfferRepository cardOfferRepository,
            StickerRepository stickerRepository,
            @Lazy ExchangeService exchangeService) {
        this.cardOfferRepository = cardOfferRepository;
        this.stickerRepository = stickerRepository;
        this.exchangeService = exchangeService;
    }

    public List<CardOfferResponse> getOffers(Long userId) {
        return cardOfferRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardOfferResponse addOffer(Long userId, CardOfferRequest request) {
        validateStickerExists(request.getStickerId());
        CardOffer cardOffer = CardOffer.builder()
                .userId(userId)
                .stickerId(request.getStickerId())
                .offerPayed(request.getOfferPayed() != null ? request.getOfferPayed() : false)
                .offerFreebie(request.getOfferFreebie() != null ? request.getOfferFreebie() : false)
                .offerExchange(request.getOfferExchange() != null ? request.getOfferExchange() : false)
                .build();
        return mapToResponse(cardOfferRepository.save(cardOffer));
    }

    @Transactional
    public List<CardOfferResponse> addBulkOffers(Long userId, BulkCardOfferRequest request) {
        request.getStickerIds().forEach(this::validateStickerExists);
        List<CardOffer> offers = request.getStickerIds().stream()
                .map(stickerId -> CardOffer.builder()
                        .userId(userId)
                        .stickerId(stickerId)
                        .offerPayed(request.getOfferPayed() != null ? request.getOfferPayed() : false)
                        .offerFreebie(request.getOfferFreebie() != null ? request.getOfferFreebie() : false)
                        .offerExchange(request.getOfferExchange() != null ? request.getOfferExchange() : false)
                        .build())
                .collect(Collectors.toList());
        return cardOfferRepository.saveAll(offers).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteOffer(Long userId, Long offerId) {
        CardOffer cardOffer = cardOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (!cardOffer.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to delete this offer");
        }

        // Cancel any exchanges that reference this card
        exchangeService.handleCardOfferDeletion(offerId);

        cardOfferRepository.delete(cardOffer);
    }

    @Transactional
    public void removeBulkOffers(Long userId, BulkCardOfferRequest request) {
        List<Long> stickerIds = request.getStickerIds();
        if (stickerIds == null || stickerIds.isEmpty()) {
            return;
        }

        List<CardOffer> userOffers = cardOfferRepository.findByUserIdAndStickerIdIn(userId, stickerIds);

        // Group user offers by stickerId for efficient matching
        var offersByStickerId = userOffers.stream()
                .collect(Collectors.groupingBy(CardOffer::getStickerId));

        List<CardOffer> toDelete = new ArrayList<>();

        // Group requested deletions by stickerId to handle duplicates
        var requestedDeletions = stickerIds.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        requestedDeletions.forEach((stickerId, count) -> {
            List<CardOffer> available = offersByStickerId.getOrDefault(stickerId, Collections.emptyList());
            // Delete up to 'count' entries
            int limit = (int) Math.min(count, available.size());
            toDelete.addAll(available.subList(0, limit));
        });

        cardOfferRepository.deleteAll(toDelete);
    }

    @Transactional
    public List<CardOfferResponse> updateBulkOffers(Long userId, BulkUpdateOfferRequest request) {
        List<Long> stickerIds = request.getStickerIds();
        if (stickerIds == null || stickerIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CardOffer> userOffers = cardOfferRepository.findByUserIdAndStickerIdIn(userId, stickerIds);

        userOffers.forEach(offer -> {
            if (request.getOfferPayed() != null)
                offer.setOfferPayed(request.getOfferPayed());
            if (request.getOfferFreebie() != null)
                offer.setOfferFreebie(request.getOfferFreebie());
            if (request.getOfferExchange() != null)
                offer.setOfferExchange(request.getOfferExchange());
        });

        return cardOfferRepository.saveAll(userOffers).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateStickerExists(Long stickerId) {
        if (!stickerRepository.existsById(stickerId)) {
            throw new IllegalArgumentException("Sticker with ID " + stickerId + " does not exist");
        }
    }

    private CardOfferResponse mapToResponse(CardOffer cardOffer) {
        return CardOfferResponse.builder()
                .id(cardOffer.getId())
                .stickerId(cardOffer.getStickerId())
                .offerPayed(cardOffer.getOfferPayed())
                .offerFreebie(cardOffer.getOfferFreebie())
                .offerExchange(cardOffer.getOfferExchange())
                .isReserved(cardOffer.getIsReserved())
                .build();
    }
}
