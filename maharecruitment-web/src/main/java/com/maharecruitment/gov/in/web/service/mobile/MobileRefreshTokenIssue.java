package com.maharecruitment.gov.in.web.service.mobile;

import java.time.Instant;

public record MobileRefreshTokenIssue(
        String refreshToken,
        Instant expiresAt,
        long expiresInSeconds) {
}
