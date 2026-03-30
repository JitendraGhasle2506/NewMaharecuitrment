package com.maharecruitment.gov.in.invoice.service.model;

import java.math.BigDecimal;

public record TaxInvoiceAmountBreakdown(
        BigDecimal baseAmount,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {
}
