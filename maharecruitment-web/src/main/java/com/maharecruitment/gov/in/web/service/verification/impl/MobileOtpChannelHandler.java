package com.maharecruitment.gov.in.web.service.verification.impl;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;

@Component
public class MobileOtpChannelHandler implements OtpChannelHandler {

    private final OtpDispatchService otpDispatchService;

    public MobileOtpChannelHandler(OtpDispatchService otpDispatchService) {
        this.otpDispatchService = otpDispatchService;
    }

    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.MOBILE;
    }

    @Override
    public String normalizeReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            throw new IllegalArgumentException("Mobile number is required.");
        }

        String normalized = reference.trim();
        if (!normalized.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits.");
        }
        return normalized;
    }

    @Override
    public void dispatchOtp(String reference, String otp) {
        otpDispatchService.sendMobileOtp(reference, otp);
    }
}
