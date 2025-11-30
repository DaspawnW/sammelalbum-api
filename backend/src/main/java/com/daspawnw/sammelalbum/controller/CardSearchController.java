package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.CardSearchDtos.BulkCardSearchRequest;
import com.daspawnw.sammelalbum.dto.CardSearchDtos.CardSearchRequest;
import com.daspawnw.sammelalbum.dto.CardSearchDtos.CardSearchResponse;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.CardSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/card-searches")
@RequiredArgsConstructor
@Tag(name = "Card Search", description = "Endpoints for managing card searches")
@SecurityRequirement(name = "bearerAuth")
public class CardSearchController {

    private final CardSearchService cardSearchService;

    @Operation(summary = "Get all searches", description = "Retrieves all card searches for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of card searches retrieved successfully")
    @GetMapping
    public ResponseEntity<List<CardSearchResponse>> getSearches(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(cardSearchService.getSearches(userDetails.getUserId()));
    }

    @Operation(summary = "Add a search", description = "Adds a single card search for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Card search added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping
    public ResponseEntity<CardSearchResponse> addSearch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CardSearchRequest request) {
        return ResponseEntity.ok(cardSearchService.addSearch(userDetails.getUserId(), request));
    }

    @Operation(summary = "Add multiple searches", description = "Adds multiple card searches for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Card searches added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping("/bulk")
    public ResponseEntity<List<CardSearchResponse>> addBulkSearches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BulkCardSearchRequest request) {
        return ResponseEntity.ok(cardSearchService.addBulkSearches(userDetails.getUserId(), request));
    }

    @Operation(summary = "Delete multiple searches", description = "Deletes multiple card searches for the authenticated user, handling duplicates intelligently")
    @ApiResponse(responseCode = "204", description = "Card searches deleted successfully")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> removeBulkSearches(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody BulkCardSearchRequest request) {
        cardSearchService.removeBulkSearches(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a search", description = "Deletes a specific card search by ID")
    @ApiResponse(responseCode = "204", description = "Card search deleted successfully")
    @ApiResponse(responseCode = "404", description = "Card search not found or not owned by user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSearch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        cardSearchService.deleteSearch(userDetails.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
