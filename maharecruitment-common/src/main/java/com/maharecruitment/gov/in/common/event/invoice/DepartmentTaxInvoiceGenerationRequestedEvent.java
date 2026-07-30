package com.maharecruitment.gov.in.common.event.invoice;

public record DepartmentTaxInvoiceGenerationRequestedEvent(
        Long departmentProjectApplicationId,
        String requestId,
        String actorEmail) {
}
