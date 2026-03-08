package com.maharecruitment.gov.in.web.service.verification.impl;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;

@Component
public class EmailOtpChannelHandler implements OtpChannelHandler {

    private final OtpDispatchService otpDispatchService;

    public EmailOtpChannelHandler(OtpDispatchService otpDispatchService) {
        this.otpDispatchService = otpDispatchService;
    }

    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.EMAIL;
    }

    @Override
    public String normalizeReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            throw new IllegalArgumentException("Email address is required.");
        }

        String normalized = reference.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return normalized;
    }

    @Override
    public void dispatchOtp(String reference, String otp) {
        otpDispatchService.sendEmailOtp(reference, otp);
    }
}
