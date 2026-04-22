package com.maharecruitment.gov.in.workorder.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;
import com.maharecruitment.gov.in.workorder.exception.WorkOrderException;
import com.maharecruitment.gov.in.workorder.service.model.GeneratedWorkOrderDocument;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderDocumentContext;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderEmployeeView;

@Service
public class WorkOrderDocxGenerator {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public GeneratedWorkOrderDocument generate(WorkOrderDocumentContext context) {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addParagraph(document, "WORK ORDER", true, 16, ParagraphAlignment.CENTER, 12);
            addKeyValueLine(document, "Ref. No.", defaultText(context.referenceNumber(), context.workOrderNumber()));
            addKeyValueLine(document, "Work Order No.", context.workOrderNumber());
            addKeyValueLine(document, "Date", formatDate(context.workOrderDate()));
            addBlankLine(document);

            addParagraph(document, "To,", false, 11, ParagraphAlignment.LEFT, 0);
            addParagraph(document,
                    defaultText(context.agencyContactName(), "Authorized Signatory") + ",",
                    false,
                    11,
                    ParagraphAlignment.LEFT,
                    0);
            addParagraph(document, defaultText(context.agencyName(), "-"), false, 11, ParagraphAlignment.LEFT, 0);
            addParagraph(document,
                    defaultText(context.agencyAddress(), "Address not available"),
                    false,
                    11,
                    ParagraphAlignment.LEFT,
                    0);
            if (StringUtils.hasText(context.agencyOfficialEmail())) {
                addParagraph(document, "Email: " + context.agencyOfficialEmail(),
                        false, 11, ParagraphAlignment.LEFT, 8);
            } else {
                addBlankLine(document);
            }

            addParagraph(document,
                    "Subject: " + defaultText(context.subjectLine(), "Work Order"),
                    true,
                    11,
                    ParagraphAlignment.LEFT,
                    8);
            addParagraph(document, buildLeadParagraph(context), false, 11, ParagraphAlignment.BOTH, 6);
            addParagraph(document, buildScopeParagraph(context), false, 11, ParagraphAlignment.BOTH, 6);

            if (StringUtils.hasText(context.purposeSummary())) {
                addParagraph(document,
                        "Additional Instructions: " + context.purposeSummary(),
                        false,
                        11,
                        ParagraphAlignment.BOTH,
                        6);
            }
            if (context.workOrderType() == WorkOrderType.EXTENSION && StringUtils.hasText(context.extensionReason())) {
                addParagraph(document,
                        "Extension Rationale: " + context.extensionReason(),
                        false,
                        11,
                        ParagraphAlignment.BOTH,
                        8);
            }

            addEmployeeTable(document, context.employees());

            addParagraph(document,
                    "This work order has been generated through the MahaRecruitment HR workflow. "
                            + "Employee deployment, attendance, compliance, and billing will be governed by the "
                            + "approved commercial and statutory terms applicable to the project.",
                    false,
                    11,
                    ParagraphAlignment.BOTH,
                    8);

            addParagraph(document,
                    "For " + defaultText(context.issuedByOrganizationName(), "MahaIT"),
                    false,
                    11,
                    ParagraphAlignment.LEFT,
                    24);
            addParagraph(document, "Authorized Signatory", true, 11, ParagraphAlignment.LEFT, 0);
            addParagraph(document,
                    defaultText(context.issuedByAddress(), "-"),
                    false,
                    10,
                    ParagraphAlignment.LEFT,
                    0);

            document.write(outputStream);
            byte[] bytes = outputStream.toByteArray();
            String fileName = sanitizeFileName(context.workOrderNumber()) + ".docx";
            return new GeneratedWorkOrderDocument(
                    fileName,
                    DOCX_CONTENT_TYPE,
                    bytes,
                    (long) bytes.length);
        } catch (IOException ex) {
            throw new WorkOrderException("Unable to generate work-order document.", ex);
        }
    }

    private void addEmployeeTable(XWPFDocument document, List<WorkOrderEmployeeView> employees) {
        List<WorkOrderEmployeeView> safeEmployees = employees == null ? List.of() : employees;
        
        // Aggregate by Designation and Level
        java.util.Map<String, List<WorkOrderEmployeeView>> aggregated = new java.util.LinkedHashMap<>();
        for (WorkOrderEmployeeView emp : safeEmployees) {
            String key = defaultText(emp.designationName(), "Unknown") + "|" + defaultText(emp.levelCode(), "-");
            aggregated.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(emp);
        }

        XWPFTable table = document.createTable(Math.max(aggregated.size(), 1) + 2, 8); // +2 for header and footer

        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("Sr. No.");
        headerRow.getCell(1).setText("Designation");
        headerRow.getCell(2).setText("Level");
        headerRow.getCell(3).setText("No.");
        headerRow.getCell(4).setText("Rate");
        headerRow.getCell(5).setText("Comm.");
        headerRow.getCell(6).setText("GST");
        headerRow.getCell(7).setText("Total");

        if (aggregated.isEmpty()) {
            XWPFTableRow row = table.getRow(1);
            row.getCell(0).setText("-");
            row.getCell(1).setText("No resources selected");
            for (int i = 2; i < 8; i++) row.getCell(i).setText("-");
            return;
        }

        int index = 1;
        java.math.BigDecimal grandTotal = java.math.BigDecimal.ZERO;
        for (java.util.Map.Entry<String, List<WorkOrderEmployeeView>> entry : aggregated.entrySet()) {
            List<WorkOrderEmployeeView> group = entry.getValue();
            WorkOrderEmployeeView first = group.get(0);
            int count = group.size();

            java.math.BigDecimal rate = first.monthlyRate() != null ? first.monthlyRate() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal comm = first.agencyCommissionAmount() != null ? first.agencyCommissionAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal gst = first.gstAmount() != null ? first.gstAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal perResourceTotal = first.totalAmount() != null ? first.totalAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal rowTotal = perResourceTotal.multiply(java.math.BigDecimal.valueOf(count));
            grandTotal = grandTotal.add(rowTotal);

            XWPFTableRow row = table.getRow(index);
            row.getCell(0).setText(String.valueOf(index));
            row.getCell(1).setText(defaultText(first.designationName(), "-"));
            row.getCell(2).setText(defaultText(first.levelCode(), "-"));
            row.getCell(3).setText(String.valueOf(count));
            row.getCell(4).setText(rate.toPlainString());
            row.getCell(5).setText(comm.toPlainString());
            row.getCell(6).setText(gst.toPlainString());
            row.getCell(7).setText(perResourceTotal.toPlainString());
            index++;
        }

        XWPFTableRow footerRow = table.getRow(index);
        footerRow.getCell(0).setText("");
        footerRow.getCell(1).setText("Total Monthly Remuneration");
        for (int i = 2; i < 7; i++) footerRow.getCell(i).setText("");
        footerRow.getCell(7).setText(grandTotal.toPlainString());
    }

    private String buildLeadParagraph(WorkOrderDocumentContext context) {
        if (context.workOrderType() == WorkOrderType.EXTENSION) {
            String parentReference = StringUtils.hasText(context.parentWorkOrderNumber())
                    ? " with reference to work order " + context.parentWorkOrderNumber()
                    : "";
            return "MahaIT hereby issues an extension of the existing deployment arrangement" + parentReference
                    + " for the technical resources assigned through request "
                    + defaultText(context.requestId(), "-")
                    + " for the project "
                    + defaultText(context.projectName(), "-")
                    + ". The validity is extended from "
                    + formatDate(context.effectiveFrom())
                    + " to "
                    + formatDate(context.effectiveTo())
                    + ".";
        }

        return "MahaIT hereby issues this work order to "
                + defaultText(context.agencyName(), "the selected agency")
                + " for deployment of approved technical resources under request "
                + defaultText(context.requestId(), "-")
                + " for the project "
                + defaultText(context.projectName(), "-")
                + ". The work order is valid from "
                + formatDate(context.effectiveFrom())
                + " to "
                + formatDate(context.effectiveTo())
                + ".";
    }

    private String buildScopeParagraph(WorkOrderDocumentContext context) {
        return "The deployment is aligned to department "
                + defaultText(context.departmentName(), "-")
                + (StringUtils.hasText(context.subDepartmentName()) ? " / " + context.subDepartmentName() : "")
                + ". The following technical manpower resources are authorized for deployment within the approved project scope.";
    }

    private void addKeyValueLine(XWPFDocument document, String key, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun keyRun = paragraph.createRun();
        keyRun.setBold(true);
        keyRun.setFontSize(11);
        keyRun.setText(key + ": ");

        XWPFRun valueRun = paragraph.createRun();
        valueRun.setFontSize(11);
        valueRun.setText(defaultText(value, "-"));
    }

    private void addParagraph(
            XWPFDocument document,
            String text,
            boolean bold,
            int fontSize,
            ParagraphAlignment alignment,
            int spacingAfter) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setSpacingAfter(spacingAfter);

        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setText(defaultText(text, ""));
    }

    private void addBlankLine(XWPFDocument document) {
        addParagraph(document, "", false, 10, ParagraphAlignment.LEFT, 6);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DISPLAY_DATE_FORMAT.format(value);
    }

    private String sanitizeFileName(String value) {
        String fileName = defaultText(value, "work-order");
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
