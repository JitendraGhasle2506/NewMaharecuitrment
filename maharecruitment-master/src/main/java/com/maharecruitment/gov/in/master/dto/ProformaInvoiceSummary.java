package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;

public record ProformaInvoiceSummary(String piNumber, BigDecimal piAmount) {
    public ProformaInvoiceSummary(String piNumber) {
        this(piNumber, BigDecimal.ZERO);
    }
}
