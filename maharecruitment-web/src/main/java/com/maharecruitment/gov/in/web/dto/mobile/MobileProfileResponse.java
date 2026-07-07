package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        String faceData,
        String tokenType,
        String accessToken,
        Long expiresIn,
        Instant expiresAt) {

    @JsonProperty("embedding")
    public String embedding() {
        return faceData;
    }
}
