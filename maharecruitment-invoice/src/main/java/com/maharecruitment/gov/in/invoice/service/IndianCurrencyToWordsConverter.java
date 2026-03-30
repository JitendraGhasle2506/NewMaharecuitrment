package com.maharecruitment.gov.in.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class IndianCurrencyToWordsConverter {

    private static final String[] ONES = {
            "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public String convert(BigDecimal amount) {
        if (amount == null) {
            return "Zero Rupees Only";
        }

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = normalizedAmount.longValue();
        int paise = normalizedAmount.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        List<String> parts = new ArrayList<>();
        parts.add(convertIntegerPart(rupees));
        parts.add(rupees == 1 ? "Rupee" : "Rupees");

        if (paise > 0) {
            parts.add("and");
            parts.add(convertIntegerPart(paise));
            parts.add("Paise");
        }

        parts.add("Only");
        return String.join(" ", parts).replaceAll("\\s+", " ").trim();
    }

    private String convertIntegerPart(long number) {
        if (number == 0) {
            return ONES[0];
        }

        List<String> parts = new ArrayList<>();
        long remaining = number;

        if (remaining >= 10_000_000L) {
            parts.add(convertIntegerPart(remaining / 10_000_000L));
            parts.add("Crore");
            remaining %= 10_000_000L;
        }

        if (remaining >= 100_000L) {
            parts.add(convertIntegerPart(remaining / 100_000L));
            parts.add("Lakh");
            remaining %= 100_000L;
        }

        if (remaining >= 1_000L) {
            parts.add(convertIntegerPart(remaining / 1_000L));
            parts.add("Thousand");
            remaining %= 1_000L;
        }

        if (remaining >= 100L) {
            parts.add(convertIntegerPart(remaining / 100L));
            parts.add("Hundred");
            remaining %= 100L;
        }

        if (remaining > 0) {
            if (!parts.isEmpty()) {
                parts.add("and");
            }
            parts.add(convertBelowHundred((int) remaining));
        }

        return String.join(" ", parts).replaceAll("\\s+", " ").trim();
    }

    private String convertBelowHundred(int number) {
        if (number < 20) {
            return ONES[number];
        }

        int tens = number / 10;
        int ones = number % 10;

        if (ones == 0) {
            return TENS[tens];
        }
        return TENS[tens] + " " + ONES[ones];
    }
}
