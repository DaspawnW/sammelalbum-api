package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.MatchDtos.MatchResponse;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Endpoints for finding potential card exchange matches")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "Get freebie matches", description = "Retrieves paginated list of users offering cards for free that match the authenticated user's searches")
    @GetMapping("/freebie")
    public ResponseEntity<Page<MatchResponse>> getFreebieMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getFreebieMatches(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "Get paid matches", description = "Retrieves paginated list of users offering cards for payment that match the authenticated user's searches")
    @GetMapping("/payed")
    public ResponseEntity<Page<MatchResponse>> getPayedMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getPayedMatches(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "Get exchange matches", description = "Retrieves paginated list of potential card-for-card exchanges with other users")
    @GetMapping("/exchange")
    public ResponseEntity<Page<MatchResponse>> getExchangeMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getExchangeMatches(userDetails.getUserId(), pageable));
    }
}
