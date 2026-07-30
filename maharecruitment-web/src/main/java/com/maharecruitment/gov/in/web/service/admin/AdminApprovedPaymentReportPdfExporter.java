package com.maharecruitment.gov.in.web.service.admin;

import com.maharecruitment.gov.in.web.service.admin.model.AdminApprovedPaymentReportPdfData;

public interface AdminApprovedPaymentReportPdfExporter {

    byte[] export(AdminApprovedPaymentReportPdfData reportData);
}
