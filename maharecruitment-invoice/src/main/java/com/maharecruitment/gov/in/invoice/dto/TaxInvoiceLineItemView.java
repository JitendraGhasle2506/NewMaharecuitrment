package com.maharecruitment.gov.in.invoice.dto;

import java.math.BigDecimal;

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
public class TaxInvoiceLineItemView {

    private Long resourceRequirementId;
    private Integer lineNumber;
    private String description;
    private String sacHsn;
    private Integer quantity;
    private Integer durationInMonths;
    private BigDecimal ratePerMonth;
    private BigDecimal totalAmount;
}
