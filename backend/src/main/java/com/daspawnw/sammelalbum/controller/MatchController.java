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

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/freebie")
    public ResponseEntity<Page<MatchResponse>> getFreebieMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getFreebieMatches(userDetails.getUserId(), pageable));
    }

    @GetMapping("/payed")
    public ResponseEntity<Page<MatchResponse>> getPayedMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getPayedMatches(userDetails.getUserId(), pageable));
    }

    @GetMapping("/exchange")
    public ResponseEntity<Page<MatchResponse>> getExchangeMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(matchService.getExchangeMatches(userDetails.getUserId(), pageable));
    }
}
