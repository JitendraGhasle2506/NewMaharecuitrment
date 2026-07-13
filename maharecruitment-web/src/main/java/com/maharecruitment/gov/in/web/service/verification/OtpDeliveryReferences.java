package com.maharecruitment.gov.in.web.service.verification;

import java.util.Locale;

import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.common.sms.util.MobileNumberUtil;

public final class OtpDeliveryReferences {

    private static final String SEPARATOR = "|";

    private OtpDeliveryReferences() {
    }

    public static String both(String email, String mobileNumber) {
        return normalizeEmail(email) + SEPARATOR + MobileNumberUtil.normalizeIndianMobileNumber(mobileNumber);
    }

    public static BothReference parseBoth(String reference) {
        if (!StringUtils.hasText(reference)) {
            throw new IllegalArgumentException("Email and mobile number are required.");
        }

        String[] parts = reference.trim().split("\\|", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Email and mobile number are required.");
        }
        return new BothReference(normalizeEmail(parts[0]), MobileNumberUtil.normalizeIndianMobileNumber(parts[1]));
    }

    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email address is required.");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return normalized;
    }

    public static String maskEmail(String email) {
        String normalized = normalizeEmail(email);
        int atIndex = normalized.indexOf('@');
        return normalized.charAt(0) + "***" + normalized.substring(atIndex);
    }

    public record BothReference(String email, String mobileNumber) {
    }
}
