package com.maharecruitment.gov.in.web.service.mobile;

import java.time.Instant;
import java.util.List;

public record MobileTokenClaims(
        String subject,
        Long userId,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt,
        String tokenId) {

    public MobileTokenClaims {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
