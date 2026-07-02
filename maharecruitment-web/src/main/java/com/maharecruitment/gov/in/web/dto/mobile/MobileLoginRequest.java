package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileLoginRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 254, message = "Username must not exceed 254 characters.")
        String username,

        @NotBlank(message = "Password is required.")
        @Size(max = 128, message = "Password must not exceed 128 characters.")
        String password) {
}
