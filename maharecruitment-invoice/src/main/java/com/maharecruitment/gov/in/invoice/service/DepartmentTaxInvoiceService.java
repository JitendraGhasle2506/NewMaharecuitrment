package com.maharecruitment.gov.in.invoice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceListItemView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;

public interface DepartmentTaxInvoiceService {

    Page<TaxInvoiceListItemView> getGeneratedInvoices(Pageable pageable);

    TaxInvoiceView getInvoiceByRequestId(String requestId);

    TaxInvoiceView getInvoiceByApplicationId(Long applicationId);

    TaxInvoiceView previewInvoiceByApplicationId(Long applicationId);

    TaxInvoiceView generateForApplication(Long applicationId, String actorEmail);

    void invalidateForApplication(Long applicationId, String actorEmail);
}
