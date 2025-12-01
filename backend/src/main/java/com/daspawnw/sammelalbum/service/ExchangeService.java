package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.CancellationReason;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.model.ExchangeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.function.Function;
import com.daspawnw.sammelalbum.model.User;

import com.daspawnw.sammelalbum.dto.ExchangeRequestDto;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.repository.StickerRepository;
import com.daspawnw.sammelalbum.service.notification.NotificationService;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final CardOfferRepository cardOfferRepository;
    private final com.daspawnw.sammelalbum.repository.CardSearchRepository cardSearchRepository;
    private final StickerRepository stickerRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public ExchangeRequest createExchangeRequest(Long requesterId, Long offererId, Long requestedStickerId,
            Long offeredStickerId, ExchangeType type) {
        log.info("createExchangeRequest called with requesterId: {}, offererId: {}", requesterId, offererId);
        if (requesterId.equals(offererId)) {
            throw new IllegalArgumentException("Cannot create exchange request with yourself");
        }

        // Check for existing active requests
        boolean exists = exchangeRequestRepository
                .existsByRequesterIdAndOffererIdAndRequestedStickerIdAndOfferedStickerIdAndExchangeTypeAndStatusIn(
                        requesterId, offererId, requestedStickerId, offeredStickerId, type,
                        List.of(ExchangeStatus.INITIAL, ExchangeStatus.MAIL_SEND, ExchangeStatus.EXCHANGE_INTERREST));

        if (exists) {
            throw new IllegalArgumentException("An active exchange request already exists for this selection");
        }

        switch (type) {
            case EXCHANGE:
                validateExchange(requesterId, offererId, requestedStickerId, offeredStickerId);
                break;
            case PAYED:
                validatePayed(requesterId, offererId, requestedStickerId, offeredStickerId);
                break;
            case FREEBIE:
                validateFreebie(requesterId, offererId, requestedStickerId, offeredStickerId);
                break;
        }

        ExchangeRequest request = ExchangeRequest.builder()
                .requesterId(requesterId)
                .offererId(offererId)
                .requestedStickerId(requestedStickerId)
                .offeredStickerId(offeredStickerId)
                .exchangeType(type)
                .status(ExchangeStatus.INITIAL)
                .build();

        ExchangeRequest saved = exchangeRequestRepository.save(request);
        log.info("Saved request with ID: {}", saved.getId());
        return saved;
    }

    private void validateExchange(Long requesterId, Long offererId, Long requestedStickerId, Long offeredStickerId) {
        if (offeredStickerId == null) {
            throw new IllegalArgumentException("Offered sticker is required for EXCHANGE type");
        }

        // 1. Validate Offerer has requestedStickerId (Exchange=true) AND Requester
        // needs it
        boolean offererHasIt = cardOfferRepository
                .findMatchingOffers(requesterId, List.of(offererId), false, false, true).stream()
                .anyMatch(offer -> offer.getSticker().getId().equals(requestedStickerId));

        // 2. Validate Requester has offeredStickerId (Exchange=true) AND Offerer needs
        // it
        boolean requesterHasIt = cardOfferRepository
                .findMatchingOffers(offererId, List.of(requesterId), false, false, true).stream()
                .anyMatch(offer -> offer.getSticker().getId().equals(offeredStickerId));

        if (!offererHasIt || !requesterHasIt) {
            throw new IllegalArgumentException(
                    "Invalid EXCHANGE: Mutual match not found or stickers not available for exchange");
        }
    }

    private void validatePayed(Long requesterId, Long offererId, Long requestedStickerId, Long offeredStickerId) {
        if (offeredStickerId != null) {
            throw new IllegalArgumentException("Offered sticker must be null for PAYED type");
        }

        // Validate Offerer has requestedStickerId (Payed=true) AND Requester needs it
        boolean valid = cardOfferRepository.findMatchingOffers(requesterId, List.of(offererId), false, true, false)
                .stream()
                .anyMatch(offer -> offer.getSticker().getId().equals(requestedStickerId));

        if (!valid) {
            throw new IllegalArgumentException("Invalid PAYED request: Offer not found or not marked as payed");
        }
    }

    private void validateFreebie(Long requesterId, Long offererId, Long requestedStickerId, Long offeredStickerId) {
        if (offeredStickerId != null) {
            throw new IllegalArgumentException("Offered sticker must be null for FREEBIE type");
        }

        // Validate Offerer has requestedStickerId (Freebie=true) AND Requester needs it
        boolean valid = cardOfferRepository.findMatchingOffers(requesterId, List.of(offererId), true, false, false)
                .stream()
                .anyMatch(offer -> offer.getSticker().getId().equals(requestedStickerId));

        if (!valid) {
            throw new IllegalArgumentException("Invalid FREEBIE request: Offer not found or not marked as freebie");
        }
    }

    @Transactional
    public void processInitialRequests() {
        List<ExchangeRequest> initialRequests = exchangeRequestRepository.findByStatus(ExchangeStatus.INITIAL);

        if (initialRequests.isEmpty()) {
            return;
        }

        Map<Long, List<ExchangeRequest>> requestsByOfferer = initialRequests.stream()
                .collect(Collectors.groupingBy(ExchangeRequest::getOffererId));

        requestsByOfferer.forEach((offererId, requests) -> {
            List<String> messages = new ArrayList<>();

            for (ExchangeRequest request : requests) {
                String message = buildMessage(request);
                if (message != null) {
                    messages.add(message);
                }

                request.setStatus(ExchangeStatus.MAIL_SEND);
                exchangeRequestRepository.save(request);
            }

            if (!messages.isEmpty()) {
                notificationService.sendExchangeNotification(offererId, messages);
            }
        });
    }

    @Transactional
    public void acceptExchangeRequest(Long requestId, Long currentUserId) {
        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request not found"));

        if (!request.getOffererId().equals(currentUserId)) {
            throw new SecurityException("You are not authorized to accept this request");
        }

        // Allow accepting from MAIL_SEND or INITIAL (though usually it should be
        // MAIL_SEND)
        if (request.getStatus() != ExchangeStatus.MAIL_SEND && request.getStatus() != ExchangeStatus.INITIAL) {
            throw new IllegalStateException("Request cannot be accepted in current status: " + request.getStatus());
        }

        // --- Reservation Logic Start ---
        // 1. Requested Sticker (Offerer gives, Requester wants)
        // Offerer gives CardOffer
        com.daspawnw.sammelalbum.model.CardOffer offererCard = cardOfferRepository
                .findByUserIdAndStickerIdIn(currentUserId, List.of(request.getRequestedStickerId()))
                .stream()
                .filter(offer -> !offer.getIsReserved()) // Find unreserved
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Offerer does not have an available (unreserved) card for this request"));

        offererCard.setIsReserved(true);
        cardOfferRepository.save(offererCard);

        // Set FK reference
        request.setOffererCardOfferId(offererCard.getId());

        // Requester wants CardSearch
        com.daspawnw.sammelalbum.model.CardSearch requesterSearch = cardSearchRepository
                .findByUserIdAndStickerIdIn(request.getRequesterId(), List.of(request.getRequestedStickerId()))
                .stream()
                .filter(search -> !search.getIsReserved()) // Find unreserved
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Requester does not have an available (unreserved) search for this request"));

        requesterSearch.setIsReserved(true);
        cardSearchRepository.save(requesterSearch);

        // Set FK reference
        request.setRequesterCardSearchId(requesterSearch.getId());

        // 2. If EXCHANGE type, Offered Sticker (Requester gives, Offerer wants)
        if (request.getExchangeType() == ExchangeType.EXCHANGE) {
            // Requester gives CardOffer
            com.daspawnw.sammelalbum.model.CardOffer requesterCard = cardOfferRepository
                    .findByUserIdAndStickerIdIn(request.getRequesterId(), List.of(request.getOfferedStickerId()))
                    .stream()
                    .filter(offer -> !offer.getIsReserved()) // Find unreserved
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Requester does not have an available (unreserved) card for this request"));

            requesterCard.setIsReserved(true);
            cardOfferRepository.save(requesterCard);

            // Set FK reference
            request.setRequesterCardOfferId(requesterCard.getId());

            // Offerer wants CardSearch
            com.daspawnw.sammelalbum.model.CardSearch offererSearch = cardSearchRepository
                    .findByUserIdAndStickerIdIn(currentUserId, List.of(request.getOfferedStickerId()))
                    .stream()
                    .filter(search -> !search.getIsReserved()) // Find unreserved
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "Offerer does not have an available (unreserved) search for this request"));

            offererSearch.setIsReserved(true);
            cardSearchRepository.save(offererSearch);

            // Set FK reference
            request.setOffererCardSearchId(offererSearch.getId());
        }
        // --- Reservation Logic End ---

        request.setStatus(ExchangeStatus.EXCHANGE_INTERREST);
        exchangeRequestRepository.save(request);

        // Send notification to Requester
        User offerer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalStateException("Offerer user not found"));

        String message = String.format(
                "Deine Tauschanfrage wurde akzeptiert!\n\nKontaktinformationen des Anbieters:\nVorname: %s\nNachname: %s\nKontakt: %s",
                offerer.getFirstname(), offerer.getLastname(), offerer.getContact());

        notificationService.sendExchangeNotification(request.getRequesterId(), List.of(message));
    }

    @Transactional
    public void declineExchangeRequest(Long requestId, Long currentUserId) {
        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request not found"));

        if (!request.getOffererId().equals(currentUserId) && !request.getRequesterId().equals(currentUserId)) {
            throw new SecurityException("You are not authorized to decline this request");
        }

        if (request.getStatus() != ExchangeStatus.INITIAL &&
                request.getStatus() != ExchangeStatus.MAIL_SEND &&
                request.getStatus() != ExchangeStatus.EXCHANGE_INTERREST) {
            throw new IllegalStateException("Request cannot be declined in current status: " + request.getStatus());
        }

        if (request.getStatus() == ExchangeStatus.EXCHANGE_INTERREST) {
            // Revert reservations using FK references
            // 1. Offerer's CardOffer
            if (request.getOffererCardOfferId() != null) {
                cardOfferRepository.findById(request.getOffererCardOfferId())
                        .ifPresent(offer -> {
                            offer.setIsReserved(false);
                            cardOfferRepository.save(offer);
                        });
            }

            // 2. Requester's CardSearch
            if (request.getRequesterCardSearchId() != null) {
                cardSearchRepository.findById(request.getRequesterCardSearchId())
                        .ifPresent(search -> {
                            search.setIsReserved(false);
                            cardSearchRepository.save(search);
                        });
            }

            // 3. For EXCHANGE type: Requester's CardOffer and Offerer's CardSearch
            if (request.getExchangeType() == ExchangeType.EXCHANGE) {
                if (request.getRequesterCardOfferId() != null) {
                    cardOfferRepository.findById(request.getRequesterCardOfferId())
                            .ifPresent(offer -> {
                                offer.setIsReserved(false);
                                cardOfferRepository.save(offer);
                            });
                }

                if (request.getOffererCardSearchId() != null) {
                    cardSearchRepository.findById(request.getOffererCardSearchId())
                            .ifPresent(search -> {
                                search.setIsReserved(false);
                                cardSearchRepository.save(search);
                            });
                }
            }
        }

        // Set cancellation reason based on who declined
        if (request.getRequesterId().equals(currentUserId)) {
            request.setCancellationReason(CancellationReason.REQUESTER_CANCELED);
        } else {
            request.setCancellationReason(CancellationReason.OFFERER_CANCELED);
        }

        request.setStatus(ExchangeStatus.EXCHANGE_CANCELED);
        exchangeRequestRepository.save(request);
    }

    @Transactional
    public void closeExchangeRequest(Long requestId, Long currentUserId) {
        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request not found"));

        // Validate authorization
        boolean isRequester = request.getRequesterId().equals(currentUserId);
        boolean isOfferer = request.getOffererId().equals(currentUserId);

        if (!isRequester && !isOfferer) {
            throw new SecurityException("You are not authorized to close this request");
        }

        // Only allow closing if exchange was accepted
        if (request.getStatus() != ExchangeStatus.EXCHANGE_INTERREST) {
            throw new IllegalStateException("Request can only be closed when in EXCHANGE_INTERREST status");
        }

        // Requester closing logic
        if (isRequester && !request.getRequesterClosed()) {
            // Delete Requester's CardSearch using FK reference
            if (request.getRequesterCardSearchId() != null) {
                cardSearchRepository.deleteById(request.getRequesterCardSearchId());
            }

            // If EXCHANGE type, delete Requester's CardOffer using FK reference
            if (request.getExchangeType() == ExchangeType.EXCHANGE && request.getRequesterCardOfferId() != null) {
                cardOfferRepository.deleteById(request.getRequesterCardOfferId());
            }

            request.setRequesterClosed(true);
        }

        // Offerer closing logic
        if (isOfferer && !request.getOffererClosed()) {
            // Delete Offerer's CardOffer using FK reference
            if (request.getOffererCardOfferId() != null) {
                cardOfferRepository.deleteById(request.getOffererCardOfferId());
            }

            // If EXCHANGE type, delete Offerer's CardSearch using FK reference
            if (request.getExchangeType() == ExchangeType.EXCHANGE && request.getOffererCardSearchId() != null) {
                cardSearchRepository.deleteById(request.getOffererCardSearchId());
            }

            request.setOffererClosed(true);
        }

        // If both parties have closed, mark as completed
        if (request.getRequesterClosed() && request.getOffererClosed()) {
            request.setStatus(ExchangeStatus.EXCHANGE_COMPLETED);
        }

        exchangeRequestRepository.save(request);
    }

    private String buildMessage(ExchangeRequest request) {
        String requestedStickerName = stickerRepository.findById(request.getRequestedStickerId())
                .map(com.daspawnw.sammelalbum.model.Sticker::getName)
                .orElse("Unknown");

        switch (request.getExchangeType()) {
            case FREEBIE:
                return String.format("Anfrage (Freebie): Dein Sticker %s (ID: %d) wird angefragt.",
                        requestedStickerName, request.getRequestedStickerId());
            case PAYED:
                return String.format("Kaufanfrage: Dein Sticker %s (ID: %d) wird angefragt.",
                        requestedStickerName, request.getRequestedStickerId());
            case EXCHANGE:
                String offeredStickerName = stickerRepository.findById(request.getOfferedStickerId())
                        .map(com.daspawnw.sammelalbum.model.Sticker::getName)
                        .orElse("Unknown");
                return String.format("Tauschanfrage: Dein Sticker %s (ID: %d) gegen Sticker %s (ID: %d).",
                        requestedStickerName, request.getRequestedStickerId(),
                        offeredStickerName, request.getOfferedStickerId());
            default:
                return null;
        }
    }

    public List<ExchangeRequestDto> getSentRequests(Long requesterId) {
        log.info("getSentRequests called for requesterId: {}", requesterId);
        List<ExchangeRequest> requests = exchangeRequestRepository.findByRequesterId(requesterId);
        log.info("Found {} requests for requesterId: {}", requests.size(), requesterId);
        Map<Long, User> partners = fetchPartners(requests, ExchangeRequest::getOffererId);
        Map<Long, String> stickerNames = fetchStickerNames(requests);

        return requests.stream()
                .map(request -> mapToDto(request, partners.get(request.getOffererId()), stickerNames))
                .collect(Collectors.toList());
    }

    public List<ExchangeRequestDto> getReceivedOffers(Long offererId) {
        List<ExchangeRequest> requests = exchangeRequestRepository.findByOffererId(offererId);
        Map<Long, User> partners = fetchPartners(requests, ExchangeRequest::getRequesterId);
        Map<Long, String> stickerNames = fetchStickerNames(requests);

        return requests.stream()
                .map(request -> mapToDto(request, partners.get(request.getRequesterId()), stickerNames))
                .collect(Collectors.toList());
    }

    private Map<Long, String> fetchStickerNames(List<ExchangeRequest> requests) {
        Set<Long> stickerIds = new java.util.HashSet<>();
        requests.forEach(r -> {
            if (r.getRequestedStickerId() != null) {
                stickerIds.add(r.getRequestedStickerId());
            }
            if (r.getOfferedStickerId() != null) {
                stickerIds.add(r.getOfferedStickerId());
            }
        });

        if (stickerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return stickerRepository.findAllById(stickerIds).stream()
                .collect(Collectors.toMap(com.daspawnw.sammelalbum.model.Sticker::getId,
                        com.daspawnw.sammelalbum.model.Sticker::getName));
    }

    private Map<Long, User> fetchPartners(List<ExchangeRequest> requests, Function<ExchangeRequest, Long> idExtractor) {
        Set<Long> partnerIds = requests.stream()
                .filter(r -> r.getStatus() == ExchangeStatus.EXCHANGE_INTERREST)
                .map(idExtractor)
                .collect(Collectors.toSet());

        if (partnerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ExchangeRequestDto mapToDto(ExchangeRequest request, User partner, Map<Long, String> stickerNames) {
        ExchangeRequestDto.ExchangeRequestDtoBuilder builder = ExchangeRequestDto.builder()
                .id(request.getId())
                .requesterId(request.getRequesterId())
                .offererId(request.getOffererId())
                .requestedStickerId(request.getRequestedStickerId())
                .offeredStickerId(request.getOfferedStickerId())
                .exchangeType(request.getExchangeType())
                .status(request.getStatus())
                .cancellationReason(request.getCancellationReason())
                .createdAt(request.getCreatedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .requesterClosed(request.getRequesterClosed())
                .offererClosed(request.getOffererClosed())
                .requestedStickerName(stickerNames.getOrDefault(request.getRequestedStickerId(), "Unknown"))
                .offeredStickerName(stickerNames.getOrDefault(request.getOfferedStickerId(), null));

        if (request.getStatus() == ExchangeStatus.EXCHANGE_INTERREST && partner != null) {
            builder.partnerFirstname(partner.getFirstname());
            builder.partnerLastname(partner.getLastname());
            builder.partnerContact(partner.getContact());
        }

        return builder.build();
    }

    /**
     * Handle CardOffer deletion - cancel any exchanges that reference this card OR
     * cancel exchanges if this was the last card of this sticker for this user
     */
    @Transactional
    public void handleCardOfferDeletion(Long cardOfferId) {
        // First, get the card being deleted to know userId and stickerId
        com.daspawnw.sammelalbum.model.CardOffer deletedCard = cardOfferRepository.findById(cardOfferId)
                .orElse(null);

        if (deletedCard == null) {
            return; // Card already deleted
        }

        Long userId = deletedCard.getUserId();
        Long stickerId = deletedCard.getStickerId();
        boolean isReserved = deletedCard.getIsReserved();

        // Find all exchanges that reference this specific CardOffer (reserved
        // exchanges)
        List<ExchangeRequest> affectedExchanges = new ArrayList<>();
        affectedExchanges.addAll(exchangeRequestRepository.findByOffererCardOfferId(cardOfferId));
        affectedExchanges.addAll(exchangeRequestRepository.findByRequesterCardOfferId(cardOfferId));

        // Cancel exchanges that directly reference this card (it's reserved)
        for (ExchangeRequest exchange : affectedExchanges) {
            CancellationReason reason = CancellationReason.OFFERED_CARD_REMOVED_BY_USER;
            unreserveCardsExcept(exchange, cardOfferId, null);
            exchange.setStatus(ExchangeStatus.EXCHANGE_CANCELED);
            exchange.setCancellationReason(reason);
            exchangeRequestRepository.save(exchange);
        }

        // If the card is NOT reserved, check if there are other cards left
        if (!isReserved) {
            // Check if there are any remaining CardOffers for this user/sticker combination
            List<com.daspawnw.sammelalbum.model.CardOffer> remainingOffers = cardOfferRepository
                    .findByUserIdAndStickerIdIn(userId, List.of(stickerId))
                    .stream()
                    .filter(offer -> !offer.getId().equals(cardOfferId)) // Exclude the one being deleted
                    .collect(Collectors.toList());

            // If no cards remain, cancel all pending exchanges for this user/sticker
            if (remainingOffers.isEmpty()) {
                // Find all INITIAL or MAIL_SEND exchanges where this user is the offerer for
                // this sticker
                List<ExchangeRequest> pendingExchanges = exchangeRequestRepository.findByOffererId(userId)
                        .stream()
                        .filter(ex -> ex.getRequestedStickerId().equals(stickerId))
                        .filter(ex -> ex.getStatus() == ExchangeStatus.INITIAL
                                || ex.getStatus() == ExchangeStatus.MAIL_SEND)
                        .filter(ex -> ex.getOffererCardOfferId() == null) // Not yet reserved
                        .collect(Collectors.toList());

                for (ExchangeRequest exchange : pendingExchanges) {
                    exchange.setStatus(ExchangeStatus.EXCHANGE_CANCELED);
                    exchange.setCancellationReason(CancellationReason.OFFERED_CARD_REMOVED_BY_USER);
                    exchangeRequestRepository.save(exchange);
                }
            }
        }
    }

    /**
     * Handle CardSearch deletion - cancel any exchanges that reference this card OR
     * cancel exchanges if this was the last search of this sticker for this user
     */
    @Transactional
    public void handleCardSearchDeletion(Long cardSearchId) {
        // First, get the card being deleted to know userId and stickerId
        com.daspawnw.sammelalbum.model.CardSearch deletedSearch = cardSearchRepository.findById(cardSearchId)
                .orElse(null);

        if (deletedSearch == null) {
            return; // Search already deleted
        }

        Long userId = deletedSearch.getUserId();
        Long stickerId = deletedSearch.getStickerId();
        boolean isReserved = deletedSearch.getIsReserved();

        // Find all exchanges that reference this specific CardSearch (reserved
        // exchanges)
        List<ExchangeRequest> affectedExchanges = new ArrayList<>();
        affectedExchanges.addAll(exchangeRequestRepository.findByRequesterCardSearchId(cardSearchId));
        affectedExchanges.addAll(exchangeRequestRepository.findByOffererCardSearchId(cardSearchId));

        // Cancel exchanges that directly reference this card (it's reserved)
        for (ExchangeRequest exchange : affectedExchanges) {
            CancellationReason reason = CancellationReason.SEARCH_CARD_REMOVED_BY_USER;
            unreserveCardsExcept(exchange, null, cardSearchId);
            exchange.setStatus(ExchangeStatus.EXCHANGE_CANCELED);
            exchange.setCancellationReason(reason);
            exchangeRequestRepository.save(exchange);
        }

        // If the search is NOT reserved, check if there are other searches left
        if (!isReserved) {
            // Check if there are any remaining CardSearches for this user/sticker
            // combination
            List<com.daspawnw.sammelalbum.model.CardSearch> remainingSearches = cardSearchRepository
                    .findByUserIdAndStickerIdIn(userId, List.of(stickerId))
                    .stream()
                    .filter(search -> !search.getId().equals(cardSearchId)) // Exclude the one being deleted
                    .collect(Collectors.toList());

            // If no searches remain, cancel all pending exchanges for this user/sticker
            if (remainingSearches.isEmpty()) {
                // Find all INITIAL or MAIL_SEND exchanges where this user is the requester for
                // this sticker
                List<ExchangeRequest> pendingExchanges = exchangeRequestRepository.findByRequesterId(userId)
                        .stream()
                        .filter(ex -> ex.getRequestedStickerId().equals(stickerId))
                        .filter(ex -> ex.getStatus() == ExchangeStatus.INITIAL
                                || ex.getStatus() == ExchangeStatus.MAIL_SEND)
                        .filter(ex -> ex.getRequesterCardSearchId() == null) // Not yet reserved
                        .collect(Collectors.toList());

                for (ExchangeRequest exchange : pendingExchanges) {
                    exchange.setStatus(ExchangeStatus.EXCHANGE_CANCELED);
                    exchange.setCancellationReason(CancellationReason.SEARCH_CARD_REMOVED_BY_USER);
                    exchangeRequestRepository.save(exchange);
                }
            }
        }
    }

    /**
     * Unreserve all cards in an exchange except the ones being deleted
     */
    private void unreserveCardsExcept(ExchangeRequest exchange, Long deletedOfferId, Long deletedSearchId) {
        // Unreserve offerer's card offer (if not the one being deleted)
        if (exchange.getOffererCardOfferId() != null && !exchange.getOffererCardOfferId().equals(deletedOfferId)) {
            cardOfferRepository.findById(exchange.getOffererCardOfferId())
                    .ifPresent(offer -> {
                        offer.setIsReserved(false);
                        cardOfferRepository.save(offer);
                    });
        }

        // Unreserve requester's card search (if not the one being deleted)
        if (exchange.getRequesterCardSearchId() != null
                && !exchange.getRequesterCardSearchId().equals(deletedSearchId)) {
            cardSearchRepository.findById(exchange.getRequesterCardSearchId())
                    .ifPresent(search -> {
                        search.setIsReserved(false);
                        cardSearchRepository.save(search);
                    });
        }

        // For EXCHANGE type: unreserve requester's offer and offerer's search
        if (exchange.getExchangeType() == ExchangeType.EXCHANGE) {
            if (exchange.getRequesterCardOfferId() != null
                    && !exchange.getRequesterCardOfferId().equals(deletedOfferId)) {
                cardOfferRepository.findById(exchange.getRequesterCardOfferId())
                        .ifPresent(offer -> {
                            offer.setIsReserved(false);
                            cardOfferRepository.save(offer);
                        });
            }

            if (exchange.getOffererCardSearchId() != null
                    && !exchange.getOffererCardSearchId().equals(deletedSearchId)) {
                cardSearchRepository.findById(exchange.getOffererCardSearchId())
                        .ifPresent(search -> {
                            search.setIsReserved(false);
                            cardSearchRepository.save(search);
                        });
            }
        }
    }
}
