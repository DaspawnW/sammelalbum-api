package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.MatchDtos.MatchResponse;
import com.daspawnw.sammelalbum.dto.MatchDtos.MatchStickerDto;
import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.model.CardSearch;
import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
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
        private final com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;
        private final com.daspawnw.sammelalbum.repository.ExchangeRequestRepository exchangeRequestRepository;

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

                System.out.println("DEBUG: populateMatchDetails - isFreebie: " + isFreebie + ", userIds: " + userIds);

                // Items Requested: What partner offers that I want (Incoming)
                Map<Long, List<MatchStickerDto>> requestedMap = cardOfferRepository
                                .findMatchingOffers(currentUserId, userIds, isFreebie, isPayed, isExchange)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                CardOffer::getUserId,
                                                Collectors.mapping(
                                                                offer -> new MatchStickerDto(
                                                                                offer.getSticker().getId(),
                                                                                offer.getSticker().getName()),
                                                                Collectors.toList())));

                System.out.println("DEBUG: requestedMap size: " + requestedMap.size());
                if (!requestedMap.isEmpty()) {
                        System.out.println("DEBUG: requestedMap keys: " + requestedMap.keySet());
                        requestedMap.forEach((k, v) -> System.out.println("DEBUG: Key: " + k + ", Val: " + v.size()));
                }

                // Items Offered: What I offer that partner wants (Outgoing)
                Map<Long, List<MatchStickerDto>> offeredMap;
                if (isExchange || isFreebie) {
                        offeredMap = cardSearchRepository
                                        .findMatchingSearches(userIds, currentUserId, isFreebie, isPayed, isExchange)
                                        .stream()
                                        .collect(Collectors.groupingBy(
                                                        CardSearch::getUserId,
                                                        Collectors.mapping(
                                                                        search -> new MatchStickerDto(
                                                                                        search.getSticker().getId(),
                                                                                        search.getSticker().getName()),
                                                                        Collectors.toList())));
                } else {
                        offeredMap = Collections.emptyMap();
                }

                // Fetch active exchange requests to filter out already requested items
                // Optimize: Only fetch requests for the users in the current page
                List<ExchangeRequest> activeRequests = exchangeRequestRepository
                                .findByRequesterIdAndOffererIdIn(currentUserId, userIds).stream()
                                .filter(req -> req.getStatus() == ExchangeStatus.INITIAL
                                                ||
                                                req.getStatus() == ExchangeStatus.MAIL_SEND
                                                ||
                                                req.getStatus() == ExchangeStatus.EXCHANGE_INTERREST)
                                .toList();

                List<MatchResponse> responseList = matches.getContent().stream()
                                .map(projection -> {
                                        MatchResponse response = mapToResponse(projection);

                                        // Filter requested items (what I want from partner)
                                        List<MatchStickerDto> requested = requestedMap
                                                        .getOrDefault(projection.getUserId(), Collections.emptyList());
                                        List<MatchStickerDto> filteredRequested = requested.stream()
                                                        .filter(item -> activeRequests.stream().noneMatch(req -> req
                                                                        .getOffererId().equals(projection.getUserId())
                                                                        &&
                                                                        req.getRequestedStickerId().equals(item.getId())
                                                                        &&
                                                                        (req.getExchangeType() == ExchangeType.FREEBIE
                                                                                        ||
                                                                                        req.getExchangeType() == ExchangeType.PAYED
                                                                                        ||
                                                                                        req.getExchangeType() == ExchangeType.EXCHANGE)))
                                                        .collect(Collectors.toList());
                                        response.setItemsRequested(filteredRequested);

                                        // Filter offered items (what I give to partner - only for EXCHANGE)
                                        List<MatchStickerDto> offered = offeredMap.getOrDefault(projection.getUserId(),
                                                        Collections.emptyList());
                                        List<MatchStickerDto> filteredOffered = offered.stream()
                                                        .filter(item -> activeRequests.stream().noneMatch(req -> req
                                                                        .getOffererId().equals(projection.getUserId())
                                                                        &&
                                                                        req.getOfferedStickerId() != null &&
                                                                        req.getOfferedStickerId().equals(item.getId())
                                                                        &&
                                                                        req.getExchangeType() == com.daspawnw.sammelalbum.model.ExchangeType.EXCHANGE))
                                                        .collect(Collectors.toList());
                                        response.setItemsOffered(filteredOffered);

                                        // Update count
                                        if (isExchange) {
                                                response.setExchangeableCount((long) Math.min(filteredRequested.size(),
                                                                filteredOffered.size()));
                                        } else {
                                                response.setExchangeableCount((long) filteredRequested.size());
                                        }

                                        return response;
                                })
                                .filter(response -> {
                                        if (isExchange) {
                                                return !response.getItemsRequested().isEmpty()
                                                                && !response.getItemsOffered().isEmpty();
                                        } else if (isFreebie) {
                                                return !response.getItemsRequested().isEmpty()
                                                                || !response.getItemsOffered().isEmpty();
                                        } else {
                                                return !response.getItemsRequested().isEmpty();
                                        }
                                })
                                .collect(Collectors.toList());

                System.out.println("DEBUG: Final responseList size: " + responseList.size());
                return new org.springframework.data.domain.PageImpl<>(responseList, matches.getPageable(),
                                matches.getTotalElements());
        }

        private MatchResponse mapToResponse(MatchProjection projection) {
                return MatchResponse.builder()
                                .userId(projection.getUserId())
                                .exchangeableCount(projection.getMatchCount())
                                .itemsRequested(Collections.emptyList())
                                .itemsOffered(Collections.emptyList())
                                .build();
        }
}
