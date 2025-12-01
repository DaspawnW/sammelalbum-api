package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeType;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.ExchangeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.daspawnw.sammelalbum.dto.ExchangeRequestDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
@Tag(name = "Exchanges", description = "Endpoints for managing card exchange requests")
@SecurityRequirement(name = "bearerAuth")
public class ExchangeController {

    private final ExchangeService exchangeService;

    @Operation(summary = "Create exchange request", description = "Creates a new exchange request between the authenticated user and another user")
    @ApiResponse(responseCode = "200", description = "Exchange request created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request (e.g., requesting from self, invalid stickers, or exchange type mismatch)")
    @PostMapping
    public ResponseEntity<ExchangeRequest> createExchangeRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateExchangeRequestDto requestDto) {
        try {
            ExchangeRequest request = exchangeService.createExchangeRequest(
                    userDetails.getUserId(),
                    requestDto.getOffererId(),
                    requestDto.getRequestedStickerId(),
                    requestDto.getOfferedStickerId(),
                    requestDto.getExchangeType());
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Accept exchange request", description = "Accepts an exchange request and reserves the involved cards")
    @ApiResponse(responseCode = "200", description = "Exchange request accepted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or exchange already accepted/declined")
    @ApiResponse(responseCode = "403", description = "User not authorized to accept this exchange")
    @PutMapping("/{id}/accept")
    public ResponseEntity<Void> acceptExchangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            exchangeService.acceptExchangeRequest(id, userDetails.getUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(summary = "Decline exchange request", description = "Declines an exchange request and unreserves any reserved cards")
    @ApiResponse(responseCode = "200", description = "Exchange request declined successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or exchange not in correct state")
    @ApiResponse(responseCode = "403", description = "User not authorized to decline this exchange")
    @PutMapping("/{id}/decline")
    public ResponseEntity<Void> declineExchangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            exchangeService.declineExchangeRequest(id, userDetails.getUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(summary = "Close exchange request", description = "Marks an exchange as completed and deletes the exchanged cards")
    @ApiResponse(responseCode = "200", description = "Exchange request closed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or exchange not in accepted state")
    @ApiResponse(responseCode = "403", description = "User not authorized to close this exchange")
    @PutMapping("/{id}/close")
    public ResponseEntity<Void> closeExchangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            exchangeService.closeExchangeRequest(id, userDetails.getUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(summary = "Get sent requests", description = "Retrieves all exchange requests sent by the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of sent exchange requests")
    @GetMapping("/sent")
    public ResponseEntity<List<ExchangeRequestDto>> getSentRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(exchangeService.getSentRequests(userDetails.getUserId()));
    }

    @Operation(summary = "Get received requests", description = "Retrieves all exchange requests received by the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of received exchange requests")
    @GetMapping("/received")
    public ResponseEntity<List<ExchangeRequestDto>> getReceivedOffers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(exchangeService.getReceivedOffers(userDetails.getUserId()));
    }

    @Data
    public static class CreateExchangeRequestDto {
        private Long offererId;
        private Long requestedStickerId;
        private Long offeredStickerId;
        private ExchangeType exchangeType;
    }
}
