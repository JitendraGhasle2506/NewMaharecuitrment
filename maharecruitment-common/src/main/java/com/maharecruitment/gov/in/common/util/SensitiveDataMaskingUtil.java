package com.maharecruitment.gov.in.common.util;

public final class SensitiveDataMaskingUtil {

    private static final String AADHAAR_MASK_PREFIX = "XXXXXXXX";
    private static final int VISIBLE_AADHAAR_DIGITS = 4;

    private SensitiveDataMaskingUtil() {
    }

    public static String maskAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.isBlank()) {
            return null;
        }

        String digitsOnly = aadhaarNumber.replaceAll("\\D", "");
        if (digitsOnly.isBlank()) {
            return AADHAAR_MASK_PREFIX;
        }

        int visibleStart = Math.max(0, digitsOnly.length() - VISIBLE_AADHAAR_DIGITS);
        return AADHAAR_MASK_PREFIX + digitsOnly.substring(visibleStart);
    }
}
