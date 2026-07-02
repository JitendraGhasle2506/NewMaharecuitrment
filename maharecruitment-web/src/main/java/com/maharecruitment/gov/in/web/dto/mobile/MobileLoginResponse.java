package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record MobileLoginResponse(
        Long userId,
        String name,
        String email,
        String mobileNo,
        List<String> roles,
        String tokenType,
        String accessToken,
        long expiresIn,
        Instant expiresAt,
        LocalDateTime loginAt,
        LocalDateTime lastLoginAt) {

    public MobileLoginResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
