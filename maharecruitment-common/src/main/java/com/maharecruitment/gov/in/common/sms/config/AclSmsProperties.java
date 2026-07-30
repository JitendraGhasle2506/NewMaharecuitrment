package com.maharecruitment.gov.in.common.sms.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "maharecruitment.sms.acl")
public record AclSmsProperties(

        @NotBlank
        String baseUrl,

        @NotBlank
        String senderId,

        @NotBlank
        String appId,

        String userId,

        String password,

        @NotNull
        Duration connectTimeout,

        @NotNull
        Duration readTimeout,

        int contentType,

        int alert,

        boolean selfId,

        @NotNull
        Duration otpValidity,

        @NotNull
        Duration resendCooldown,

        int maximumVerificationAttempts,

        int maximumResendAttempts
) {
}
