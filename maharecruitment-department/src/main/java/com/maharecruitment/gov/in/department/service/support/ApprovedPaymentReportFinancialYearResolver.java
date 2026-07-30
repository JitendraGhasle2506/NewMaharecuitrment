package com.maharecruitment.gov.in.department.service.support;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApprovedPaymentReportFinancialYearResolver {

    private static final Pattern FINANCIAL_YEAR_PATTERN = Pattern.compile("^(\\d{4})\\s*-\\s*(\\d{2}|\\d{4})$");

    private ApprovedPaymentReportFinancialYearResolver() {
    }

    public static FinancialYearRange resolve(String financialYear) {
        if (financialYear == null || financialYear.isBlank()) {
            return null;
        }

        Matcher matcher = FINANCIAL_YEAR_PATTERN.matcher(financialYear.trim());
        if (!matcher.matches()) {
            return null;
        }

        int startYear = Integer.parseInt(matcher.group(1));
        String endYearToken = matcher.group(2);
        int endYear = endYearToken.length() == 2
                ? (startYear / 100) * 100 + Integer.parseInt(endYearToken)
                : Integer.parseInt(endYearToken);

        if (endYear != startYear + 1) {
            return null;
        }

        return new FinancialYearRange(
                LocalDate.of(startYear, 4, 1),
                LocalDate.of(endYear, 3, 31));
    }

    public record FinancialYearRange(LocalDate startDate, LocalDate endDate) {
    }
}
