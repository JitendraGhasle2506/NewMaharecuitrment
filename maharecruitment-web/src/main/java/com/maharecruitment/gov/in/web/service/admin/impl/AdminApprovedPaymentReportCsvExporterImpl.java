package com.maharecruitment.gov.in.web.service.admin.impl;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.web.service.admin.AdminApprovedPaymentReportCsvExporter;

@Service
public class AdminApprovedPaymentReportCsvExporterImpl implements AdminApprovedPaymentReportCsvExporter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Override
    public byte[] export(List<ApprovedPaymentReportRowView> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",",
                "Payment ID",
                "Request ID",
                "Department",
                "Project Name",
                "Project Code",
                "Work Order Number",
                "Proforma Invoice",
                "Receipt Number",
                "UTR Number",
                "Payment Mode",
                "Approved Amount",
                "Approved Status",
                "Approved Date",
                "Approved By",
                "Created Date",
                "Remarks"))
                .append(System.lineSeparator());

        for (ApprovedPaymentReportRowView row : rows) {
            csv.append(Stream.of(
                    escape(row.getPaymentId()),
                    escape(row.getRequestId()),
                    escape(row.getDepartmentName()),
                    escape(row.getProjectName()),
                    escape(row.getProjectCode()),
                    escape(row.getWorkOrderNumber()),
                    escape(row.getProformaInvoiceId()),
                    escape(row.getReceiptNumber()),
                    escape(row.getUtrNumber()),
                    escape(row.getPaymentMode()),
                    escape(row.getTotalAmount()),
                    escape(row.getApplicationStatus() == null ? null : row.getApplicationStatus().getDisplayName()),
                    escape(row.getApprovedDate() == null ? null : DATE_TIME_FORMATTER.format(row.getApprovedDate())),
                    escape(row.getApprovedBy()),
                    escape(row.getCreatedDate() == null ? null : DATE_TIME_FORMATTER.format(row.getCreatedDate())),
                    escape(row.getRemarks()))
                    .collect(Collectors.joining(",")));
            csv.append(System.lineSeparator());
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(Object value) {
        String text = value == null ? "" : value.toString();
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
