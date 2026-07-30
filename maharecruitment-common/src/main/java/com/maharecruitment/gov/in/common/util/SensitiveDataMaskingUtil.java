package com.maharecruitment.gov.in.common.util;

public final class SensitiveDataMaskingUtil {

    private SensitiveDataMaskingUtil() {
    }

    public static String maskAadhaar(String aadhaarNumber) {
        return maskKeepingLastFour(normalizeAadhaar(aadhaarNumber));
    }

    public static String normalizeAadhaar(String aadhaarNumber) {
        return aadhaarNumber == null ? null : aadhaarNumber.replaceAll("\\D", "");
    }

    public static String maskPan(String panNumber) {
        return maskKeepingLastFour(normalizeText(panNumber));
    }

    public static String maskGst(String gstNumber) {
        return maskKeepingLastFour(normalizeText(gstNumber));
    }

    public static String maskKeepingLastFour(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= 4) {
            return "X".repeat(normalized.length());
        }
        return "X".repeat(normalized.length() - 4) + normalized.substring(normalized.length() - 4);
    }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
