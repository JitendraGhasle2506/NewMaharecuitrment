package com.maharecruitment.gov.in.auth.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UserValidationUtil {

    private static final int NAME_MAX_LENGTH = 150;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 100;
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

    private UserValidationUtil() {
    }

    public static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User name is required.");
        }

        String normalized = value.trim();
        if (normalized.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("User name must not exceed 150 characters.");
        }

        return normalized;
    }

    public static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException("Email must not exceed 255 characters.");
        }

        return normalized;
    }

    public static String normalizeOptionalMobile(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (!MOBILE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Mobile number must be 10 to 15 digits.");
        }

        return normalized;
    }

    public static String validatePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException("Password must not start or end with spaces.");
        }

        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Password must not contain spaces.");
        }

        if (value.length() < PASSWORD_MIN_LENGTH || value.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("Password must be between 8 and 100 characters.");
        }

        boolean hasUpper = value.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = value.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(
                    "Password must include at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        return value;
    }
}
