package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.model.*;
import com.daspawnw.sammelalbum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for handling user account deletion.
 * Delegates to domain services to ensure proper separation of concerns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeletionService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final CardOfferRepository cardOfferRepository;
    private final CardSearchRepository cardSearchRepository;
    private final CredentialsRepository credentialsRepository;
    private final UserRepository userRepository;

    /**
     * Deletes a user and all associated data.
     * 
     * Process:
     * 1. Delete all exchanges involving the user (required for FK constraints)
     * 2. Delete ALL user's card offers (both reserved and unreserved)
     * 3. Delete ALL user's card searches (both reserved and unreserved)
     * 4. Delete user's credentials (cascades to user entity)
     * 
     * @param userId ID of the user to delete
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Starting deletion process for user ID: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            log.warn("User with ID {} not found, skipping deletion", userId);
            return;
        }

        // Step 1: Delete all exchanges involving this user
        // Note: Exchanges must be deleted (not just closed) due to FK constraints
        deleteAllUserExchanges(userId);

        // Step 2: Delete ALL card offers (reserved and unreserved)
        deleteAllUserOffers(userId);

        // Step 3: Delete ALL card searches (reserved and unreserved)
        deleteAllUserSearches(userId);

        // Step 4: Delete credentials (which cascades to user)
        deleteUserAndCredentials(userId);

        log.info("Successfully deleted user ID: {}", userId);
    }

    /**
     * Deletes all exchanges where the user is involved (as requester or offerer).
     * Unreserves partner's items in active exchanges before deletion.
     * 
     * Note: Exchanges must be deleted (not just closed) to allow user deletion
     * due to foreign key constraints on requester_id and offerer_id.
     */
    private void deleteAllUserExchanges(Long userId) {
        log.debug("Deleting all exchanges for user ID: {}", userId);

        // Find all exchanges where user is requester
        List<ExchangeRequest> asRequester = exchangeRequestRepository.findByRequesterId(userId);

        // Find all exchanges where user is offerer
        List<ExchangeRequest> asOfferer = exchangeRequestRepository.findByOffererId(userId);

        // Process exchanges where user is requester
        for (ExchangeRequest exchange : asRequester) {
            // Unreserve partner's items if exchange is active
            if (exchange.getStatus() != ExchangeStatus.EXCHANGE_CANCELED
                    && exchange.getStatus() != ExchangeStatus.EXCHANGE_COMPLETED) {
                unreserveOffererItems(exchange);
                log.debug("Unreserved partner items for exchange ID {} where user was requester", exchange.getId());
            }
            // Delete the exchange
            exchangeRequestRepository.delete(exchange);
        }

        // Process exchanges where user is offerer
        for (ExchangeRequest exchange : asOfferer) {
            // Unreserve partner's items if exchange is active
            if (exchange.getStatus() != ExchangeStatus.EXCHANGE_CANCELED
                    && exchange.getStatus() != ExchangeStatus.EXCHANGE_COMPLETED) {
                unreserveRequesterItems(exchange);
                log.debug("Unreserved partner items for exchange ID {} where user was offerer", exchange.getId());
            }
            // Delete the exchange
            exchangeRequestRepository.delete(exchange);
        }

        log.debug("Deleted {} exchanges where user was requester and {} where user was offerer",
                asRequester.size(), asOfferer.size());
    }

    /**
     * Unreserves the offerer's items in an exchange.
     * Called when the requester (user being deleted) is removed.
     */
    private void unreserveOffererItems(ExchangeRequest exchange) {
        // Unreserve offerer's card offer
        if (exchange.getOffererCardOfferId() != null) {
            cardOfferRepository.findById(exchange.getOffererCardOfferId())
                    .ifPresent(offer -> {
                        offer.setIsReserved(false);
                        cardOfferRepository.save(offer);
                        log.debug("Unreserved offerer's card offer ID: {}", offer.getId());
                    });
        }

        // For EXCHANGE type: unreserve offerer's card search
        if (exchange.getExchangeType() == ExchangeType.EXCHANGE
                && exchange.getOffererCardSearchId() != null) {
            cardSearchRepository.findById(exchange.getOffererCardSearchId())
                    .ifPresent(search -> {
                        search.setIsReserved(false);
                        cardSearchRepository.save(search);
                        log.debug("Unreserved offerer's card search ID: {}", search.getId());
                    });
        }
    }

    /**
     * Unreserves the requester's items in an exchange.
     * Called when the offerer (user being deleted) is removed.
     */
    private void unreserveRequesterItems(ExchangeRequest exchange) {
        // Unreserve requester's card search
        if (exchange.getRequesterCardSearchId() != null) {
            cardSearchRepository.findById(exchange.getRequesterCardSearchId())
                    .ifPresent(search -> {
                        search.setIsReserved(false);
                        cardSearchRepository.save(search);
                        log.debug("Unreserved requester's card search ID: {}", search.getId());
                    });
        }

        // For EXCHANGE type: unreserve requester's card offer
        if (exchange.getExchangeType() == ExchangeType.EXCHANGE
                && exchange.getRequesterCardOfferId() != null) {
            cardOfferRepository.findById(exchange.getRequesterCardOfferId())
                    .ifPresent(offer -> {
                        offer.setIsReserved(false);
                        cardOfferRepository.save(offer);
                        log.debug("Unreserved requester's card offer ID: {}", offer.getId());
                    });
        }
    }

    /**
     * Deletes ALL card offers belonging to the user.
     * This includes both reserved and unreserved offers.
     */
    private void deleteAllUserOffers(Long userId) {
        List<CardOffer> offers = cardOfferRepository.findAllByUserId(userId);
        int count = offers.size();

        if (count > 0) {
            cardOfferRepository.deleteAll(offers);
            log.debug("Deleted {} card offers for user ID: {}", count, userId);
        }
    }

    /**
     * Deletes ALL card searches belonging to the user.
     * This includes both reserved and unreserved searches.
     */
    private void deleteAllUserSearches(Long userId) {
        List<CardSearch> searches = cardSearchRepository.findAllByUserId(userId);
        int count = searches.size();

        if (count > 0) {
            cardSearchRepository.deleteAll(searches);
            log.debug("Deleted {} card searches for user ID: {}", count, userId);
        }
    }

    /**
     * Deletes the user's credentials and user entity.
     * Credentials deletion cascades to user due to CascadeType.ALL.
     */
    private void deleteUserAndCredentials(Long userId) {
        // Find and delete credentials (which cascades to user)
        credentialsRepository.findAll().stream()
                .filter(cred -> cred.getUser() != null && cred.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(credentials -> {
                    credentialsRepository.delete(credentials);
                    log.debug("Deleted credentials and user for user ID: {}", userId);
                });
    }
}
