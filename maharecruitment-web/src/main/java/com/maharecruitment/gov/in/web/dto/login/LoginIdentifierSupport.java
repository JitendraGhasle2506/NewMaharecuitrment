package com.maharecruitment.gov.in.web.dto.login;

import java.util.regex.Pattern;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

final class LoginIdentifierSupport {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");

    private LoginIdentifierSupport() {
    }

    static VerificationChannel inferChannel(String identifier) {
        String normalized = normalize(identifier);
        if (EMAIL_PATTERN.matcher(normalized).matches()) {
            return VerificationChannel.EMAIL;
        }
        if (MOBILE_PATTERN.matcher(normalized).matches()) {
            return VerificationChannel.SMS;
        }
        return null;
    }

    static boolean isEmailOrMobile(String identifier) {
        return inferChannel(identifier) != null;
    }

    static String normalize(String identifier) {
        return identifier == null ? "" : identifier.trim();
    }
}
