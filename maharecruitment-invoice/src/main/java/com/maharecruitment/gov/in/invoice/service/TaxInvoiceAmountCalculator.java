package com.maharecruitment.gov.in.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.invoice.service.model.TaxInvoiceAmountBreakdown;

@Component
public class TaxInvoiceAmountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public TaxInvoiceAmountBreakdown calculate(BigDecimal baseAmount, BigDecimal cgstRate, BigDecimal sgstRate) {
        BigDecimal normalizedBaseAmount = normalizeCurrency(baseAmount);
        BigDecimal normalizedCgstRate = normalizeRate(cgstRate);
        BigDecimal normalizedSgstRate = normalizeRate(sgstRate);

        BigDecimal cgstAmount = calculateTaxAmount(normalizedBaseAmount, normalizedCgstRate);
        BigDecimal sgstAmount = calculateTaxAmount(normalizedBaseAmount, normalizedSgstRate);
        BigDecimal taxAmount = cgstAmount.add(sgstAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = normalizedBaseAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return new TaxInvoiceAmountBreakdown(
                normalizedBaseAmount,
                normalizedCgstRate,
                cgstAmount,
                normalizedSgstRate,
                sgstAmount,
                taxAmount,
                totalAmount);
    }

    private BigDecimal calculateTaxAmount(BigDecimal baseAmount, BigDecimal ratePercentage) {
        if (baseAmount.compareTo(ZERO) <= 0 || ratePercentage.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return baseAmount.multiply(ratePercentage)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCurrency(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
