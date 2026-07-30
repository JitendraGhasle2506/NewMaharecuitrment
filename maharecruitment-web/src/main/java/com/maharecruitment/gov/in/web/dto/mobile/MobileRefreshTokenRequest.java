package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileRefreshTokenRequest(
        @NotBlank(message = "Refresh token is required.")
        @Size(max = 512, message = "Refresh token is invalid.")
        String refreshToken) {
}
