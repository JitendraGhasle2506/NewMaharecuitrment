package com.maharecruitment.gov.in.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaxInvoiceGenerationOptionView {

    private final Long id;
    private final String name;
    private final String code;
    private final Long departmentId;
    private final Long subDepartmentId;
}
