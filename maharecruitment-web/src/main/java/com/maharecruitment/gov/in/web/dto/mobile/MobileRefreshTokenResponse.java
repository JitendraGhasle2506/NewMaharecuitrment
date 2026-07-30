package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.Instant;

public record MobileRefreshTokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        Instant expiresAt,
        String refreshToken,
        long refreshExpiresIn,
        Instant refreshExpiresAt) {
}
