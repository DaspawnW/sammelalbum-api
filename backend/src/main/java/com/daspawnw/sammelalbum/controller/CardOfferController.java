package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkCardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.BulkUpdateOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferRequest;
import com.daspawnw.sammelalbum.dto.CardOfferDtos.CardOfferResponse;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.CardOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/card-offers")
@RequiredArgsConstructor
@Tag(name = "Card Offers", description = "Endpoints for managing card offers")
@SecurityRequirement(name = "bearerAuth")
public class CardOfferController {

    private final CardOfferService cardOfferService;

    @Operation(summary = "Get all offers", description = "Retrieves all card offers for the authenticated user")
    @GetMapping
    public ResponseEntity<List<CardOfferResponse>> getOffers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(cardOfferService.getOffers(userDetails.getUserId()));
    }

    @Operation(summary = "Add an offer", description = "Adds a single card offer for the authenticated user")
    @PostMapping
    public ResponseEntity<CardOfferResponse> addOffer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CardOfferRequest request) {
        return ResponseEntity.ok(cardOfferService.addOffer(userDetails.getUserId(), request));
    }

    @Operation(summary = "Add multiple offers", description = "Adds multiple card offers for the authenticated user")
    @PostMapping("/bulk")
    public ResponseEntity<List<CardOfferResponse>> addBulkOffers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BulkCardOfferRequest request) {
        return ResponseEntity.ok(cardOfferService.addBulkOffers(userDetails.getUserId(), request));
    }

    @Operation(summary = "Delete multiple offers", description = "Deletes multiple card offers for the authenticated user, handling duplicates intelligently")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> removeBulkOffers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BulkCardOfferRequest request) {
        cardOfferService.removeBulkOffers(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update multiple offers", description = "Updates the offer type for all offers matching the given sticker IDs for the authenticated user")
    @PutMapping("/bulk")
    public ResponseEntity<List<CardOfferResponse>> updateBulkOffers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BulkUpdateOfferRequest request) {
        return ResponseEntity.ok(cardOfferService.updateBulkOffers(userDetails.getUserId(), request));
    }

    @Operation(summary = "Delete an offer", description = "Deletes a specific card offer by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        cardOfferService.deleteOffer(userDetails.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
