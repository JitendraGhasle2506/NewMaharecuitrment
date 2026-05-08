package com.maharecruitment.gov.in.web.service.admin.model;

import java.time.LocalDateTime;
import java.util.List;

import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;

public record AdminApprovedPaymentReportPdfData(
        String reportTitle,
        String departmentScope,
        String financialYearLabel,
        String approvedDateRangeLabel,
        LocalDateTime generatedAt,
        ApprovedPaymentReportSummaryView summary,
        List<ApprovedPaymentReportRowView> rows) {
}
