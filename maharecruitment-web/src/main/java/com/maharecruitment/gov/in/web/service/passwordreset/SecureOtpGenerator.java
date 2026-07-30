package com.maharecruitment.gov.in.web.service.passwordreset;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class SecureOtpGenerator {

    private static final int OTP_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSixDigitOtp() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
