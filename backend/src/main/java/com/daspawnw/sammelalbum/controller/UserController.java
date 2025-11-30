package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.UserDto;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Endpoints for user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;

    @Operation(summary = "Get current user", description = "Retrieves the profile of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    @ApiResponse(responseCode = "403", description = "User not authorized")
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .mail(user.getMail())
                .contact(user.getContact())
                .build();

        return ResponseEntity.ok(userDto);
    }
}
