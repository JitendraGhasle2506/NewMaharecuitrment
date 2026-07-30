package com.maharecruitment.gov.in.common.sms.util;

public final class MobileNumberUtil {

    private MobileNumberUtil() {
    }

    public static String normalizeIndianMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        String normalized = mobileNumber
                .replaceAll("\\s+", "")
                .replace("-", "");

        if (normalized.startsWith("+91")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("91") && normalized.length() == 12) {
            normalized = normalized.substring(2);
        }

        if (!normalized.matches("[6-9]\\d{9}")) {
            throw new IllegalArgumentException("Invalid Indian mobile number");
        }

        return normalized;
    }

    public static String mask(String mobileNumber) {
        String normalized = normalizeIndianMobileNumber(mobileNumber);
        return "******" + normalized.substring(6);
    }
}
