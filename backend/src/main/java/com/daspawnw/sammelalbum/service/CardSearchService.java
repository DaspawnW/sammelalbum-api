package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.CardSearchDtos.BulkCardSearchRequest;
import com.daspawnw.sammelalbum.dto.CardSearchDtos.CardSearchRequest;
import com.daspawnw.sammelalbum.dto.CardSearchDtos.CardSearchResponse;
import com.daspawnw.sammelalbum.model.CardSearch;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardSearchService {

    private final CardSearchRepository cardSearchRepository;
    private final StickerRepository stickerRepository;
    private final ExchangeService exchangeService;

    public CardSearchService(CardSearchRepository cardSearchRepository,
            StickerRepository stickerRepository,
            @Lazy ExchangeService exchangeService) {
        this.cardSearchRepository = cardSearchRepository;
        this.stickerRepository = stickerRepository;
        this.exchangeService = exchangeService;
    }

    public List<CardSearchResponse> getSearches(Long userId) {
        return cardSearchRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardSearchResponse addSearch(Long userId, CardSearchRequest request) {
        validateStickerExists(request.getStickerId());
        CardSearch cardSearch = CardSearch.builder()
                .userId(userId)
                .stickerId(request.getStickerId())
                .build();
        return mapToResponse(cardSearchRepository.save(cardSearch));
    }

    @Transactional
    public List<CardSearchResponse> addBulkSearches(Long userId, BulkCardSearchRequest request) {
        request.getStickerIds().forEach(this::validateStickerExists);
        List<CardSearch> searches = request.getStickerIds().stream()
                .map(stickerId -> CardSearch.builder()
                        .userId(userId)
                        .stickerId(stickerId)
                        .build())
                .collect(Collectors.toList());
        return cardSearchRepository.saveAll(searches).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSearch(Long userId, Long searchId) {
        CardSearch cardSearch = cardSearchRepository.findById(searchId)
                .orElseThrow(() -> new IllegalArgumentException("Search not found"));

        if (!cardSearch.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to delete this search");
        }

        // Cancel any exchanges that reference this card
        exchangeService.handleCardSearchDeletion(searchId);

        cardSearchRepository.delete(cardSearch);
    }

    @Transactional
    public void removeBulkSearches(Long userId, BulkCardSearchRequest request) {
        List<Long> stickerIds = request.getStickerIds();
        if (stickerIds == null || stickerIds.isEmpty()) {
            return;
        }

        List<CardSearch> userSearches = cardSearchRepository.findByUserIdAndStickerIdIn(userId, stickerIds);

        // Group user searches by stickerId for efficient matching
        var searchesByStickerId = userSearches.stream()
                .collect(Collectors.groupingBy(CardSearch::getStickerId));

        List<CardSearch> toDelete = new java.util.ArrayList<>();

        // Group requested deletions by stickerId to handle duplicates (e.g., delete 233
        // twice)
        var requestedDeletions = stickerIds.stream()
                .collect(Collectors.groupingBy(java.util.function.Function.identity(), Collectors.counting()));

        requestedDeletions.forEach((stickerId, count) -> {
            List<CardSearch> available = searchesByStickerId.getOrDefault(stickerId, java.util.Collections.emptyList());
            // Delete up to 'count' entries
            int limit = (int) Math.min(count, available.size());
            toDelete.addAll(available.subList(0, limit));
        });

        cardSearchRepository.deleteAll(toDelete);
    }

    private void validateStickerExists(Long stickerId) {
        if (!stickerRepository.existsById(stickerId)) {
            throw new IllegalArgumentException("Sticker with ID " + stickerId + " does not exist");
        }
    }

    private CardSearchResponse mapToResponse(CardSearch cardSearch) {
        return CardSearchResponse.builder()
                .id(cardSearch.getId())
                .stickerId(cardSearch.getStickerId())
                .isReserved(cardSearch.getIsReserved())
                .build();
    }
}
