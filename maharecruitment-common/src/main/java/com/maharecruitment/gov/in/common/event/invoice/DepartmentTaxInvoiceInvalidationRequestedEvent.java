package com.maharecruitment.gov.in.common.event.invoice;

public record DepartmentTaxInvoiceInvalidationRequestedEvent(
        Long departmentProjectApplicationId,
        String requestId,
        String actorEmail) {
}
