package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxInvoiceListItemView {

    private Long departmentTaxInvoiceId;
    private Long departmentProjectApplicationId;
    private String requestId;
    private String tiNumber;
    private LocalDate tiDate;
    private String projectName;
    private String billedTo;
    private BigDecimal totalAmount;
    private Long generatedByUserId;
    private String generatedByName;
    private String generatedByLoginId;
    private LocalDateTime generatedOn;
    private LocalDateTime updatedDate;
}
