package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.MatchDtos.MatchResponse;
import com.daspawnw.sammelalbum.dto.MatchDtos.MatchStickerDto;
import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.MatchProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final CardOfferRepository cardOfferRepository;

    @Transactional(readOnly = true)
    public Page<MatchResponse> getFreebieMatches(Long userId, Pageable pageable) {
        Page<MatchProjection> matches = cardOfferRepository.findFreebieMatches(userId, pageable);
        return populateMatchDetails(userId, matches, true, false, false);
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> getPayedMatches(Long userId, Pageable pageable) {
        Page<MatchProjection> matches = cardOfferRepository.findPayedMatches(userId, pageable);
        return populateMatchDetails(userId, matches, false, true, false);
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> getExchangeMatches(Long userId, Pageable pageable) {
        Page<MatchProjection> matches = cardOfferRepository.findExchangeMatches(userId, pageable);
        return populateMatchDetails(userId, matches, false, false, true);
    }

    private Page<MatchResponse> populateMatchDetails(Long currentUserId, Page<MatchProjection> matches,
            boolean isFreebie, boolean isPayed, boolean isExchange) {
        if (matches.isEmpty()) {
            return matches.map(this::mapToResponse);
        }

        List<Long> userIds = matches.getContent().stream()
                .map(MatchProjection::getUserId)
                .toList();

        Map<Long, List<MatchStickerDto>> detailsMap = cardOfferRepository
                .findMatchingOffers(currentUserId, userIds, isFreebie, isPayed, isExchange).stream()
                .collect(Collectors.groupingBy(
                        CardOffer::getUserId,
                        Collectors.mapping(
                                offer -> new MatchStickerDto(
                                        offer.getSticker().getId(),
                                        offer.getSticker().getName()),
                                Collectors.toList())));

        return matches.map(projection -> {
            MatchResponse response = mapToResponse(projection);
            response.setMatches(detailsMap.getOrDefault(projection.getUserId(), Collections.emptyList()));
            return response;
        });
    }

    private MatchResponse mapToResponse(MatchProjection projection) {
        return MatchResponse.builder()
                .userId(projection.getUserId())
                .matchCount(projection.getMatchCount())
                .matches(Collections.emptyList())
                .build();
    }
}
