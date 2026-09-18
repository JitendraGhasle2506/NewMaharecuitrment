package com.maharecruitment.gov.in.invoice.service;

import java.util.List;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationOptionView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationPreviewView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationResultView;

public interface DepartmentTaxInvoiceGenerationService {

    List<TaxInvoiceGenerationOptionView> getDepartmentOptions();

    List<TaxInvoiceGenerationOptionView> getSubDepartmentOptions(Long departmentId);

    List<TaxInvoiceGenerationOptionView> getProjectOptions(Long departmentId, Long subDepartmentId);

    TaxInvoiceGenerationPreviewView preview(TaxInvoiceGenerationFilter filter);

    TaxInvoiceGenerationResultView generate(TaxInvoiceGenerationFilter filter, String actorEmail);
}
