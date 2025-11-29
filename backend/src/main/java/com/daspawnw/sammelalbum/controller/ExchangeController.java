package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

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

    @GetMapping("/sent")
    public ResponseEntity<List<ExchangeRequestDto>> getSentRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(exchangeService.getSentRequests(userDetails.getUserId()));
    }

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
        private com.daspawnw.sammelalbum.model.ExchangeType exchangeType;
    }
}
