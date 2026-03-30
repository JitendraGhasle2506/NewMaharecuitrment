package com.maharecruitment.gov.in.invoice.service.model;

import java.math.BigDecimal;

public final class TaxInvoiceAmountBreakdown {

    private final BigDecimal baseAmount;
    private final BigDecimal cgstRate;
    private final BigDecimal cgstAmount;
    private final BigDecimal sgstRate;
    private final BigDecimal sgstAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;

    public TaxInvoiceAmountBreakdown(
            BigDecimal baseAmount,
            BigDecimal cgstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstRate,
            BigDecimal sgstAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount) {
        this.baseAmount = baseAmount;
        this.cgstRate = cgstRate;
        this.cgstAmount = cgstAmount;
        this.sgstRate = sgstRate;
        this.sgstAmount = sgstAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    public BigDecimal baseAmount() {
        return baseAmount;
    }

    public BigDecimal cgstRate() {
        return cgstRate;
    }

    public BigDecimal cgstAmount() {
        return cgstAmount;
    }

    public BigDecimal sgstRate() {
        return sgstRate;
    }

    public BigDecimal sgstAmount() {
        return sgstAmount;
    }

    public BigDecimal taxAmount() {
        return taxAmount;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }
}
