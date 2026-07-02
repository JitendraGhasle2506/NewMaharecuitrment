package com.maharecruitment.gov.in.web.service.mobile;

import java.time.Instant;

public record MobileTokenIssue(
        String accessToken,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt,
        long expiresInSeconds) {
}
