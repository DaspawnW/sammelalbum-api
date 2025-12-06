package com.daspawnw.sammelalbum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate password reset process")
public class ForgotPasswordRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or email address", example = "john.doe@example.com")
    private String identifier;
}
