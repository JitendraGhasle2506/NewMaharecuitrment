package com.maharecruitment.gov.in.auth.util;

import java.security.SecureRandom;

public final class SecurePasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*!?";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurePasswordGenerator() {
    }

    public static String generate(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters.");
        }

        StringBuilder builder = new StringBuilder(length);
        builder.append(randomChar(UPPER));
        builder.append(randomChar(LOWER));
        builder.append(randomChar(DIGITS));
        builder.append(randomChar(SYMBOLS));

        for (int index = builder.length(); index < length; index++) {
            builder.append(randomChar(ALL));
        }

        return shuffle(builder.toString());
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }

    private static String shuffle(String value) {
        char[] chars = value.toCharArray();
        for (int index = chars.length - 1; index > 0; index--) {
            int swapIndex = RANDOM.nextInt(index + 1);
            char temp = chars[index];
            chars[index] = chars[swapIndex];
            chars[swapIndex] = temp;
        }
        return new String(chars);
    }
}
