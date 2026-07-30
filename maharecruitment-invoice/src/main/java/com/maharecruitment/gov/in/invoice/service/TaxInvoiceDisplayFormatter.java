package com.maharecruitment.gov.in.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class TaxInvoiceDisplayFormatter {

    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    public String formatAmount(BigDecimal amount) {
        return format(amount, "#,##0.00");
    }

    public String formatRate(BigDecimal rate) {
        return format(rate, "#,##0.0000");
    }

    private String format(BigDecimal value, String pattern) {
        DecimalFormat formatter = new DecimalFormat(pattern, SYMBOLS);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setGroupingUsed(true);

        if (value == null) {
            return formatter.format(BigDecimal.ZERO);
        }

        return formatter.format(value);
    }
}
