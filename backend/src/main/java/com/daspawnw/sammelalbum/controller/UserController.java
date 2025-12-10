package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.ChangePasswordRequest;
import com.daspawnw.sammelalbum.dto.UpdateProfileRequest;
import com.daspawnw.sammelalbum.dto.UserDto;
import com.daspawnw.sammelalbum.service.UserDeletionService;
import com.daspawnw.sammelalbum.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Endpoints for user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserDeletionService userDeletionService;

    @Operation(summary = "Get current user", description = "Retrieves the profile of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    @ApiResponse(responseCode = "403", description = "User not authorized")
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto userDto = userService.getUserProfile(userDetails.getUserId());
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Update user profile", description = "Updates the profile information of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "403", description = "User not authorized")
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change password", description = "Changes the password of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid current password")
    @ApiResponse(responseCode = "403", description = "User not authorized")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete current user", description = "Permanently deletes the authenticated user's account and all associated data. This action cannot be undone.")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "403", description = "User not authorized")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userDeletionService.deleteUser(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
