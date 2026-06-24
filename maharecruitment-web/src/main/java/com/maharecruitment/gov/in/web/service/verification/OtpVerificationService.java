package com.maharecruitment.gov.in.web.service.verification;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.servlet.http.HttpSession;

public interface OtpVerificationService {

    OtpVerificationResult sendOtp(
            HttpSession session,
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context);

    OtpVerificationResult verifyOtp(
            HttpSession session,
            String purpose,
            VerificationChannel channel,
            String reference,
            String otp,
            String captchaId,
            String captchaAnswer,
            OtpRequestContext context);

    void recordUnknownSendAttempt(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context);

    void recordUnknownVerifyAttempt(
            String purpose,
            VerificationChannel channel,
            String reference,
            OtpRequestContext context);

    boolean isVerified(HttpSession session, String purpose, VerificationChannel channel, String reference);

    void clear(HttpSession session, String purpose);

    void clear(HttpSession session, String purpose, VerificationChannel channel);
}
