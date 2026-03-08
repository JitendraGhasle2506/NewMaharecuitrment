package com.maharecruitment.gov.in.web.service.verification;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

import jakarta.servlet.http.HttpSession;

public interface OtpVerificationService {

    void sendOtp(HttpSession session, String purpose, VerificationChannel channel, String reference);

    boolean verifyOtp(HttpSession session, String purpose, VerificationChannel channel, String reference, String otp);

    boolean isVerified(HttpSession session, String purpose, VerificationChannel channel, String reference);

    void clear(HttpSession session, String purpose);

    void clear(HttpSession session, String purpose, VerificationChannel channel);
}
