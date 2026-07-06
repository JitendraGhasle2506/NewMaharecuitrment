package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.Instant;

public record MobileProfileResponse(
        boolean success,
        String message,
        Long userId,
        Long employeeId,
        String employeeCode,
        String name,
        String email,
        String mobileNo,
        String photoUrl,
        String tokenType,
        String accessToken,
        Long expiresIn,
        Instant expiresAt) {
}
