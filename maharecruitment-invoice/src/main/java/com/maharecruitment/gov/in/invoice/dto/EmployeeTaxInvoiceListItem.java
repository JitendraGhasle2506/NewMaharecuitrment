package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeTaxInvoiceListItem(Long id, String tiNumber, LocalDate tiDate,
        String requestId, String projectName, String billedTo, LocalDate periodStart,
        LocalDate periodEnd, BigDecimal totalAmount, LocalDateTime generatedOn) {
}
