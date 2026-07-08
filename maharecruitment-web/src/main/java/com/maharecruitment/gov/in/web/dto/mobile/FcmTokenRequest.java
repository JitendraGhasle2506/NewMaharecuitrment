package com.maharecruitment.gov.in.web.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @NotBlank(message = "FCM token is required.")
        @Size(max = 4096, message = "FCM token must not exceed 4096 characters.")
        String fcmToken,

        @NotBlank(message = "Platform is required.")
        @Size(max = 30, message = "Platform must not exceed 30 characters.")
        String platform,

        @NotBlank(message = "Device ID is required.")
        @Size(max = 255, message = "Device ID must not exceed 255 characters.")
        String deviceId) {
}
