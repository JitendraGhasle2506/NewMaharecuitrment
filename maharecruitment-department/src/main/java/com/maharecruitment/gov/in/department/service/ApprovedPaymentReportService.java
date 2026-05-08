package com.maharecruitment.gov.in.department.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportFilter;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;
import com.maharecruitment.gov.in.department.service.model.DepartmentPaymentReportDepartmentOptionView;

public interface ApprovedPaymentReportService {

    Page<ApprovedPaymentReportRowView> getApprovedPayments(ApprovedPaymentReportFilter filter, Pageable pageable);

    List<ApprovedPaymentReportRowView> getApprovedPaymentsForExport(ApprovedPaymentReportFilter filter);

    ApprovedPaymentReportSummaryView getApprovedPaymentSummary(ApprovedPaymentReportFilter filter);

    List<DepartmentPaymentReportDepartmentOptionView> getDepartmentOptions();
}
