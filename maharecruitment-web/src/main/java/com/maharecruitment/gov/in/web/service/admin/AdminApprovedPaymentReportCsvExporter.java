package com.maharecruitment.gov.in.web.service.admin;

import java.util.List;

import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;

public interface AdminApprovedPaymentReportCsvExporter {

    byte[] export(List<ApprovedPaymentReportRowView> rows);
}
