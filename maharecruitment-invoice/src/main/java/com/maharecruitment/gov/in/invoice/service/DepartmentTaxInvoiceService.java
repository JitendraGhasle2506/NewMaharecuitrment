package com.maharecruitment.gov.in.invoice.service;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;

public interface DepartmentTaxInvoiceService {

    TaxInvoiceView getInvoiceByRequestId(String requestId);

    TaxInvoiceView getInvoiceByApplicationId(Long applicationId);

    TaxInvoiceView generateForApplication(Long applicationId, String actorEmail);
}
