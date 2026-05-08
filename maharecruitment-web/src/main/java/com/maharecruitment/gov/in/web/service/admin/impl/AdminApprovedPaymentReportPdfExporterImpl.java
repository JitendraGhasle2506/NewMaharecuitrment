package com.maharecruitment.gov.in.web.service.admin.impl;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;
import com.maharecruitment.gov.in.web.service.admin.AdminApprovedPaymentReportPdfExporter;
import com.maharecruitment.gov.in.web.service.admin.model.AdminApprovedPaymentReportPdfData;

@Service
public class AdminApprovedPaymentReportPdfExporterImpl implements AdminApprovedPaymentReportPdfExporter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final int MAX_LINES_PER_PAGE = 44;
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int LEFT_MARGIN = 28;
    private static final int START_Y = 560;
    private static final int LINE_HEIGHT = 12;

    @Override
    public byte[] export(AdminApprovedPaymentReportPdfData reportData) {
        List<String> lines = buildLines(reportData);
        List<String> pages = paginate(lines);
        return buildPdfDocument(pages);
    }

    private List<String> buildLines(AdminApprovedPaymentReportPdfData reportData) {
        List<String> lines = new ArrayList<>();
        lines.add(center(reportData.reportTitle(), 140));
        lines.add(center("Administrative register of auditor-approved payment transactions", 140));
        lines.add(repeat('=', 140));
        lines.add("Department Scope : " + safeOrDash(reportData.departmentScope()));
        lines.add("Financial Year  : " + safeOrDash(reportData.financialYearLabel()));
        lines.add("Approved Dates  : " + safeOrDash(reportData.approvedDateRangeLabel()));
        lines.add("Generated On    : " + formatDateTime(reportData.generatedAt()));
        lines.add(repeat('-', 140));
        lines.add(buildSummaryLine(reportData.summary()));
        lines.add(repeat('-', 140));
        lines.add(buildHeaderLine());
        lines.add(repeat('-', 140));

        List<ApprovedPaymentReportRowView> rows = reportData.rows();
        if (rows == null || rows.isEmpty()) {
            lines.add("No approved payment records found for the selected filters.");
            return lines;
        }

        int rowNumber = 1;
        for (ApprovedPaymentReportRowView row : rows) {
            lines.add(buildDataLine(rowNumber, row));
            rowNumber++;
        }
        return lines;
    }

    private List<String> paginate(List<String> lines) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int lineCount = 0;

        for (String line : lines) {
            if (lineCount == MAX_LINES_PER_PAGE) {
                pages.add(page.toString());
                page = new StringBuilder();
                lineCount = 0;
            }
            if (page.length() > 0) {
                page.append('\n');
            }
            page.append(line);
            lineCount++;
        }

        if (page.length() > 0) {
            pages.add(page.toString());
        }
        return pages;
    }

    private byte[] buildPdfDocument(List<String> pages) {
        List<byte[]> objects = new ArrayList<>();
        objects.add(pdfObject(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(pdfObject(2, "<< /Type /Pages /Kids [" + buildPageReferences(pages.size()) + "] /Count " + pages.size() + " >>"));
        objects.add(pdfObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"));

        int objectNumber = 4;
        for (String pageContent : pages) {
            int pageObjectNumber = objectNumber++;
            int contentObjectNumber = objectNumber++;

            objects.add(pdfObject(
                    pageObjectNumber,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
                            + "] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObjectNumber + " 0 R >>"));

            byte[] streamBytes = buildContentStream(pageContent);
            objects.add(pdfStreamObject(contentObjectNumber, streamBytes));
        }

        return assemblePdf(objects);
    }

    private String buildPageReferences(int pageCount) {
        StringBuilder references = new StringBuilder();
        int pageObjectNumber = 4;
        for (int index = 0; index < pageCount; index++) {
            if (references.length() > 0) {
                references.append(' ');
            }
            references.append(pageObjectNumber).append(" 0 R");
            pageObjectNumber += 2;
        }
        return references.toString();
    }

    private byte[] buildContentStream(String pageContent) {
        String[] lines = pageContent.split("\n", -1);
        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 8 Tf\n");
        content.append(LINE_HEIGHT).append(" TL\n");
        content.append(LEFT_MARGIN).append(' ').append(START_Y).append(" Td\n");
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                content.append("T*\n");
            }
            content.append('(')
                    .append(escapePdfText(lines[index]))
                    .append(") Tj\n");
        }
        content.append("ET\n");
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] assemblePdf(List<byte[]> objects) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);

            for (byte[] object : objects) {
                offsets.add(outputStream.size());
                outputStream.write(object);
            }

            int xrefOffset = outputStream.size();
            outputStream.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
            for (int index = 1; index < offsets.size(); index++) {
                outputStream.write(String.format("%010d 00000 n %n", offsets.get(index)).getBytes(StandardCharsets.US_ASCII));
            }

            outputStream.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(("startxref\n" + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
            return outputStream.toByteArray();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to generate approved payment report PDF.", exception);
        }
    }

    private byte[] pdfObject(int objectNumber, String body) {
        String object = objectNumber + " 0 obj\n" + body + "\nendobj\n";
        return object.getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] pdfStreamObject(int objectNumber, byte[] streamBytes) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write((objectNumber + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(("<< /Length " + streamBytes.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(streamBytes);
            outputStream.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            return outputStream.toByteArray();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to generate approved payment report PDF.", exception);
        }
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return "INR " + AMOUNT_FORMAT.format(safeAmount);
    }

    private String buildSummaryLine(ApprovedPaymentReportSummaryView summary) {
        long totalPayments = summary == null ? 0L : summary.getTotalPayments();
        long totalDepartments = summary == null ? 0L : summary.getTotalDepartments();
        BigDecimal totalAmount = summary == null ? BigDecimal.ZERO : summary.getTotalApprovedAmount();

        return padRight("Approved Payments: " + totalPayments, 34)
                + padRight("Approved Amount: " + formatAmount(totalAmount), 48)
                + "Departments Covered: " + totalDepartments;
    }

    private String buildHeaderLine() {
        return padRight("Sr", 4)
                + padRight("Request / Payment", 22)
                + padRight("Department / Project", 34)
                + padRight("Invoice / Receipt / UTR", 32)
                + padRight("Mode", 10)
                + padRight("Amount", 16)
                + padRight("Approved On", 18)
                + "Approved By";
    }

    private String buildDataLine(int rowNumber, ApprovedPaymentReportRowView row) {
        String requestAndPayment = safeOrDash(row.getRequestId()) + " / " + safe(row.getPaymentId());
        String departmentAndProject = resolveDepartmentName(row) + " / " + compactProjectLabel(row);
        String invoiceReceiptUtr = safeOrDash(row.getProformaInvoiceId()) + " / "
                + safeOrDash(row.getReceiptNumber()) + " / "
                + safeOrDash(row.getUtrNumber());

        return padRight(String.valueOf(rowNumber), 4)
                + padRight(requestAndPayment, 22)
                + padRight(departmentAndProject, 34)
                + padRight(invoiceReceiptUtr, 32)
                + padRight(safeOrDash(row.getPaymentMode()), 10)
                + padRight(formatAmount(row.getTotalAmount()), 16)
                + padRight(formatDateTime(row.getApprovedDate()), 18)
                + truncate(safeOrDash(row.getApprovedBy()), 18);
    }

    private String compactProjectLabel(ApprovedPaymentReportRowView row) {
        String projectName = safeOrDash(row.getProjectName());
        if (hasText(row.getProjectCode())) {
            return projectName + " [" + row.getProjectCode().trim() + "]";
        }
        return projectName;
    }

    private String resolveDepartmentName(ApprovedPaymentReportRowView row) {
        if (row == null) {
            return "-";
        }
        if (hasText(row.getDepartmentName())) {
            return row.getDepartmentName().trim();
        }
        return row.getDepartmentRegistrationId() == null ? "-" : "Department #" + row.getDepartmentRegistrationId();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String center(String value, int width) {
        String safeValue = safe(value);
        if (safeValue.length() >= width) {
            return safeValue;
        }
        int leftPadding = (width - safeValue.length()) / 2;
        return repeat(' ', leftPadding) + safeValue;
    }

    private String padRight(String value, int width) {
        String truncated = truncate(safe(value), width);
        if (truncated.length() >= width) {
            return truncated;
        }
        return truncated + repeat(' ', width - truncated.length());
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safe(value);
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        if (maxLength <= 3) {
            return safeValue.substring(0, maxLength);
        }
        return safeValue.substring(0, maxLength - 3) + "...";
    }

    private String repeat(char character, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(character).repeat(count);
    }

    private String escapePdfText(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String safeOrDash(String value) {
        return hasText(value) ? value.trim() : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
