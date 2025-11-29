package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.model.ExchangeType;
import lombok.RequiredArgsConstructor;
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
        if (requesterId.equals(offererId)) {
            throw new IllegalArgumentException("Cannot create exchange request with yourself");
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

        return exchangeRequestRepository.save(request);
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
            // Revert reservations
            // 1. Requested Sticker (The sticker the Requester WANTS and Offerer GIVES)
            // Offerer's Offer for RequestedSticker
            cardOfferRepository
                    .findByUserIdAndStickerIdIn(request.getOffererId(), List.of(request.getRequestedStickerId()))
                    .stream()
                    .filter(com.daspawnw.sammelalbum.model.CardOffer::getIsReserved) // Find reserved
                    .findFirst() // Only unreserve ONE item
                    .ifPresent(offer -> {
                        offer.setIsReserved(false);
                        cardOfferRepository.save(offer);
                    });

            // Requester's Search for RequestedSticker
            cardSearchRepository
                    .findByUserIdAndStickerIdIn(request.getRequesterId(), List.of(request.getRequestedStickerId()))
                    .stream()
                    .filter(com.daspawnw.sammelalbum.model.CardSearch::getIsReserved) // Find reserved
                    .findFirst() // Only unreserve ONE item
                    .ifPresent(search -> {
                        search.setIsReserved(false);
                        cardSearchRepository.save(search);
                    });

            // 2. If EXCHANGE type, Offered Sticker (The sticker the Requester GIVES and
            // Offerer WANTS)
            // This is a DIFFERENT sticker than the Requested Sticker (usually)
            if (request.getExchangeType() == ExchangeType.EXCHANGE) {
                // Requester's Offer for OfferedSticker
                cardOfferRepository
                        .findByUserIdAndStickerIdIn(request.getRequesterId(), List.of(request.getOfferedStickerId()))
                        .stream()
                        .filter(com.daspawnw.sammelalbum.model.CardOffer::getIsReserved) // Find reserved
                        .findFirst() // Only unreserve ONE item
                        .ifPresent(offer -> {
                            offer.setIsReserved(false);
                            cardOfferRepository.save(offer);
                        });

                // Offerer's Search for OfferedSticker
                cardSearchRepository
                        .findByUserIdAndStickerIdIn(request.getOffererId(), List.of(request.getOfferedStickerId()))
                        .stream()
                        .filter(com.daspawnw.sammelalbum.model.CardSearch::getIsReserved) // Find reserved
                        .findFirst() // Only unreserve ONE item
                        .ifPresent(search -> {
                            search.setIsReserved(false);
                            cardSearchRepository.save(search);
                        });
            }
        }

        request.setStatus(ExchangeStatus.EXCHANGE_CANCELED);
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
        List<ExchangeRequest> requests = exchangeRequestRepository.findByRequesterId(requesterId);
        Map<Long, User> partners = fetchPartners(requests, ExchangeRequest::getOffererId);

        return requests.stream()
                .map(request -> mapToDto(request, partners.get(request.getOffererId())))
                .collect(Collectors.toList());
    }

    public List<ExchangeRequestDto> getReceivedOffers(Long offererId) {
        List<ExchangeRequest> requests = exchangeRequestRepository.findByOffererId(offererId);
        Map<Long, User> partners = fetchPartners(requests, ExchangeRequest::getRequesterId);

        return requests.stream()
                .map(request -> mapToDto(request, partners.get(request.getRequesterId())))
                .collect(Collectors.toList());
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

    private ExchangeRequestDto mapToDto(ExchangeRequest request, User partner) {
        ExchangeRequestDto.ExchangeRequestDtoBuilder builder = ExchangeRequestDto.builder()
                .id(request.getId())
                .requesterId(request.getRequesterId())
                .offererId(request.getOffererId())
                .requestedStickerId(request.getRequestedStickerId())
                .offeredStickerId(request.getOfferedStickerId())
                .exchangeType(request.getExchangeType())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt());

        if (request.getStatus() == ExchangeStatus.EXCHANGE_INTERREST && partner != null) {
            builder.partnerFirstname(partner.getFirstname());
            builder.partnerLastname(partner.getLastname());
            builder.partnerContact(partner.getContact());
        }

        return builder.build();
    }
}
