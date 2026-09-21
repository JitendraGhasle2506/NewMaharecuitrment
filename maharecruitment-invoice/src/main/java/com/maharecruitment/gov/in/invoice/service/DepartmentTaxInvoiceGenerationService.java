package com.maharecruitment.gov.in.invoice.service;

import java.util.List;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceEmployeePreviewView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationOptionView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationPreviewView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationResultView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;

public interface DepartmentTaxInvoiceGenerationService {

    List<TaxInvoiceGenerationOptionView> getDepartmentOptions();

    List<TaxInvoiceGenerationOptionView> getSubDepartmentOptions(Long departmentId);

    List<TaxInvoiceGenerationOptionView> getProjectOptions(Long departmentId, Long subDepartmentId);

    TaxInvoiceGenerationPreviewView preview(TaxInvoiceGenerationFilter filter);

    TaxInvoiceGenerationResultView generate(TaxInvoiceGenerationFilter filter, String actorEmail);

    /** Employees mapped to the selected project, i.e. the rows the employee invoice will bill. */
    List<TaxInvoiceEmployeePreviewView> loadProjectEmployees(TaxInvoiceGenerationFilter filter);

    TaxInvoiceView buildEmployeeInvoice(TaxInvoiceGenerationFilter filter);
}
