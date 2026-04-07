package com.maharecruitment.gov.in.workorder.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;
import com.maharecruitment.gov.in.workorder.exception.WorkOrderException;
import com.maharecruitment.gov.in.workorder.service.model.GeneratedWorkOrderDocument;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderDocumentContext;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderEmployeeView;

@Service
public class WorkOrderPdfGenerator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final float PAGE_WIDTH = 595F;
    private static final float PAGE_HEIGHT = 842F;
    private static final float LEFT_MARGIN = 45F;
    private static final float TOP_MARGIN = 50F;
    private static final float BOTTOM_MARGIN = 50F;
    private static final float DEFAULT_LINE_HEIGHT = 13F;
    private static final float TABLE_ROW_HEIGHT = 22F;
    private static final float TABLE_TEXT_TOP_PADDING = 14F;
    private static final float TABLE_CELL_PADDING = 4F;
    private static final float[] EMPLOYEE_TABLE_COLUMN_WIDTHS = { 28F, 75F, 130F, 142F, 50F, 80F };

    public GeneratedWorkOrderDocument generate(WorkOrderDocumentContext context) {
        try {
            byte[] bytes = writePdf(buildDocumentElements(context));
            return new GeneratedWorkOrderDocument(
                    sanitizeFileName(context.workOrderNumber()) + ".pdf",
                    PDF_CONTENT_TYPE,
                    bytes,
                    (long) bytes.length);
        } catch (IOException ex) {
            throw new WorkOrderException("Unable to generate work-order PDF document.", ex);
        }
    }

    private List<PdfElement> buildDocumentElements(WorkOrderDocumentContext context) {
        List<PdfElement> elements = new ArrayList<>();
        addLine(elements, "WORK ORDER", 16, PdfFont.BOLD, PdfAlign.CENTER, 12F);
        addLine(elements, "Ref. No.: " + defaultText(context.referenceNumber(), context.workOrderNumber()), 10,
                PdfFont.BOLD, PdfAlign.LEFT, 2F);
        addLine(elements, "Work Order No.: " + defaultText(context.workOrderNumber(), "-"), 10,
                PdfFont.BOLD, PdfAlign.LEFT, 2F);
        addLine(elements, "Date: " + formatDate(context.workOrderDate()), 10, PdfFont.BOLD, PdfAlign.LEFT, 8F);

        addLine(elements, "To,", 10, PdfFont.REGULAR, PdfAlign.LEFT, 2F);
        addLine(elements, defaultText(context.agencyContactName(), "Authorized Signatory") + ",", 10,
                PdfFont.REGULAR, PdfAlign.LEFT, 2F);
        addLine(elements, defaultText(context.agencyName(), "-"), 10, PdfFont.REGULAR, PdfAlign.LEFT, 2F);
        addWrappedLines(elements, defaultText(context.agencyAddress(), "Address not available"), 88, 10,
                PdfFont.REGULAR, PdfAlign.LEFT, 2F);
        if (StringUtils.hasText(context.agencyOfficialEmail())) {
            addLine(elements, "Email: " + context.agencyOfficialEmail(), 10, PdfFont.REGULAR, PdfAlign.LEFT, 8F);
        } else {
            addLine(elements, " ", 10, PdfFont.REGULAR, PdfAlign.LEFT, 8F);
        }

        addWrappedLines(elements, "Subject: " + defaultText(context.subjectLine(), "Work Order"), 88, 10,
                PdfFont.BOLD, PdfAlign.LEFT, 8F);
        addWrappedLines(elements, buildLeadParagraph(context), 100, 10, PdfFont.REGULAR, PdfAlign.LEFT, 6F);
        addWrappedLines(elements, buildScopeParagraph(context), 100, 10, PdfFont.REGULAR, PdfAlign.LEFT, 8F);

        if (StringUtils.hasText(context.purposeSummary())) {
            addWrappedLines(elements, "Additional Instructions: " + context.purposeSummary(), 100, 10,
                    PdfFont.REGULAR, PdfAlign.LEFT, 6F);
        }
        if (context.workOrderType() == WorkOrderType.EXTENSION && StringUtils.hasText(context.extensionReason())) {
            addWrappedLines(elements, "Extension Rationale: " + context.extensionReason(), 100, 10,
                    PdfFont.REGULAR, PdfAlign.LEFT, 8F);
        }

        addLine(elements, "Employee Mapping", 11, PdfFont.BOLD, PdfAlign.LEFT, 4F);
        addEmployeeTable(elements, context.employees());

        addWrappedLines(elements,
                "This work order has been generated through the MahaRecruitment HR workflow. "
                        + "Employee deployment, attendance, compliance, and billing will be governed by the "
                        + "approved commercial and statutory terms applicable to the project.",
                100, 10, PdfFont.REGULAR, PdfAlign.LEFT, 14F);
        addLine(elements, "For " + defaultText(context.issuedByOrganizationName(), "MahaIT"), 10,
                PdfFont.REGULAR, PdfAlign.LEFT, 24F);
        addLine(elements, "Authorized Signatory", 10, PdfFont.BOLD, PdfAlign.LEFT, 2F);
        addWrappedLines(elements, defaultText(context.issuedByAddress(), "-"), 100, 9, PdfFont.REGULAR,
                PdfAlign.LEFT, 0F);
        return elements;
    }

    private void addEmployeeTable(List<PdfElement> elements, List<WorkOrderEmployeeView> employees) {
        List<WorkOrderEmployeeView> safeEmployees = employees == null ? List.of() : employees;
        elements.add(new PdfTableRow(
                List.of("Sr", "Code", "Employee", "Designation", "Level", "Joining"),
                true,
                0F));

        if (safeEmployees.isEmpty()) {
            elements.add(new PdfTableRow(
                    List.of("-", "-", "No employees selected", "-", "-", "-"),
                    false,
                    8F));
            return;
        }

        for (int index = 0; index < safeEmployees.size(); index++) {
            WorkOrderEmployeeView employee = safeEmployees.get(index);
            elements.add(new PdfTableRow(
                    List.of(
                            String.valueOf(index + 1),
                            defaultText(employee.employeeCode(), "-"),
                            defaultText(employee.employeeName(), "-"),
                            defaultText(employee.designationName(), "-"),
                            defaultText(employee.levelCode(), "-"),
                            formatDate(employee.joiningDate())),
                    false,
                    index == safeEmployees.size() - 1 ? 10F : 0F));
        }
    }

    private byte[] writePdf(List<PdfElement> elements) throws IOException {
        List<List<PositionedPdfElement>> pages = paginate(elements);
        List<PdfObject> objects = new ArrayList<>();

        StringBuilder kids = new StringBuilder();
        int nextObjectId = 7;
        for (List<PositionedPdfElement> page : pages) {
            int contentObjectId = nextObjectId++;
            int pageObjectId = nextObjectId++;
            kids.append(pageObjectId).append(" 0 R ");

            byte[] content = buildPageContent(page);
            objects.add(new PdfObject(contentObjectId, "<< /Length " + content.length + " >>\nstream\n"
                    + new String(content, StandardCharsets.US_ASCII) + "endstream"));
            objects.add(new PdfObject(pageObjectId, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH
                    + " " + PAGE_HEIGHT + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R /F4 6 0 R >> >>"
                    + " /Contents " + contentObjectId + " 0 R >>"));
        }

        objects.add(0, new PdfObject(6, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>"));
        objects.add(0, new PdfObject(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"));
        objects.add(0, new PdfObject(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));
        objects.add(0, new PdfObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(0, new PdfObject(2, "<< /Type /Pages /Kids [" + kids.toString().trim()
                + "] /Count " + pages.size() + " >>"));
        objects.add(0, new PdfObject(1, "<< /Type /Catalog /Pages 2 0 R >>"));

        return serializePdf(objects, nextObjectId - 1);
    }

    private List<List<PositionedPdfElement>> paginate(List<PdfElement> elements) {
        List<List<PositionedPdfElement>> pages = new ArrayList<>();
        List<PositionedPdfElement> currentPage = new ArrayList<>();
        float y = PAGE_HEIGHT - TOP_MARGIN;

        for (PdfElement element : elements) {
            float elementHeight = element.height();
            if (y - elementHeight < BOTTOM_MARGIN && !currentPage.isEmpty()) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                y = PAGE_HEIGHT - TOP_MARGIN;
            }

            float x = element instanceof PdfLine line ? resolveX(line) : LEFT_MARGIN;
            currentPage.add(new PositionedPdfElement(element, x, y));
            y -= elementHeight + element.spacingAfter();
        }

        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }
        return pages.isEmpty() ? List.of(List.of(new PositionedPdfElement(
                new PdfLine("No work-order content available.", 10, PdfFont.REGULAR, PdfAlign.LEFT, 0F),
                LEFT_MARGIN,
                PAGE_HEIGHT - TOP_MARGIN))) : pages;
    }

    private float resolveX(PdfLine line) {
        if (line.align() != PdfAlign.CENTER) {
            return LEFT_MARGIN;
        }
        float estimatedWidth = sanitizePdfText(line.text()).length() * line.fontSize() * 0.45F;
        return Math.max(LEFT_MARGIN, (PAGE_WIDTH - estimatedWidth) / 2F);
    }

    private byte[] buildPageContent(List<PositionedPdfElement> pageElements) {
        StringBuilder content = new StringBuilder();
        for (PositionedPdfElement positionedElement : pageElements) {
            if (positionedElement.element() instanceof PdfLine line) {
                appendText(content, line.text(), positionedElement.x(), positionedElement.y(), line.fontSize(), line.font());
            } else if (positionedElement.element() instanceof PdfTableRow tableRow) {
                appendEmployeeTableRow(content, tableRow, positionedElement.x(), positionedElement.y());
            }
        }
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private void appendEmployeeTableRow(StringBuilder content, PdfTableRow row, float x, float y) {
        float tableWidth = 0F;
        for (float columnWidth : EMPLOYEE_TABLE_COLUMN_WIDTHS) {
            tableWidth += columnWidth;
        }

        content.append("0.7 w\n");
        content.append(formatPdfNumber(x)).append(' ')
                .append(formatPdfNumber(y - TABLE_ROW_HEIGHT)).append(' ')
                .append(formatPdfNumber(tableWidth)).append(' ')
                .append(formatPdfNumber(TABLE_ROW_HEIGHT)).append(" re S\n");

        float nextX = x;
        for (int index = 0; index < EMPLOYEE_TABLE_COLUMN_WIDTHS.length - 1; index++) {
            nextX += EMPLOYEE_TABLE_COLUMN_WIDTHS[index];
            content.append(formatPdfNumber(nextX)).append(' ')
                    .append(formatPdfNumber(y - TABLE_ROW_HEIGHT)).append(" m ")
                    .append(formatPdfNumber(nextX)).append(' ')
                    .append(formatPdfNumber(y)).append(" l S\n");
        }

        float cellX = x;
        for (int index = 0; index < row.cells().size() && index < EMPLOYEE_TABLE_COLUMN_WIDTHS.length; index++) {
            appendText(
                    content,
                    truncateForColumn(row.cells().get(index), EMPLOYEE_TABLE_COLUMN_WIDTHS[index], row.header()),
                    cellX + TABLE_CELL_PADDING,
                    y - TABLE_TEXT_TOP_PADDING,
                    row.header() ? 8 : 7,
                    row.header() ? PdfFont.BOLD : PdfFont.REGULAR);
            cellX += EMPLOYEE_TABLE_COLUMN_WIDTHS[index];
        }
    }

    private void appendText(StringBuilder content, String text, float x, float y, int fontSize, PdfFont font) {
        content.append("BT\n");
        content.append(font.resourceName()).append(' ').append(fontSize).append(" Tf\n");
        content.append("1 0 0 1 ")
                .append(formatPdfNumber(x))
                .append(' ')
                .append(formatPdfNumber(y))
                .append(" Tm\n");
        content.append('(').append(escapePdfText(text)).append(") Tj\n");
        content.append("ET\n");
    }

    private byte[] serializePdf(List<PdfObject> objects, int highestObjectId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        long[] offsets = new long[highestObjectId + 1];
        writeAscii(outputStream, "%PDF-1.4\n");
        for (PdfObject object : objects) {
            offsets[object.id()] = outputStream.size();
            writeAscii(outputStream, object.id() + " 0 obj\n");
            writeAscii(outputStream, object.content());
            writeAscii(outputStream, "\nendobj\n");
        }

        long xrefOffset = outputStream.size();
        writeAscii(outputStream, "xref\n");
        writeAscii(outputStream, "0 " + (highestObjectId + 1) + "\n");
        writeAscii(outputStream, "0000000000 65535 f \n");
        for (int objectId = 1; objectId <= highestObjectId; objectId++) {
            writeAscii(outputStream, String.format(Locale.ROOT, "%010d 00000 n \n", offsets[objectId]));
        }
        writeAscii(outputStream, "trailer\n");
        writeAscii(outputStream, "<< /Size " + (highestObjectId + 1) + " /Root 1 0 R >>\n");
        writeAscii(outputStream, "startxref\n");
        writeAscii(outputStream, xrefOffset + "\n");
        writeAscii(outputStream, "%%EOF");
        return outputStream.toByteArray();
    }

    private void addWrappedLines(
            List<PdfElement> lines,
            String text,
            int maxLineLength,
            int fontSize,
            PdfFont font,
            PdfAlign align,
            float spacingAfter) {
        List<String> wrappedLines = wrap(defaultText(text, "-"), maxLineLength);
        for (int index = 0; index < wrappedLines.size(); index++) {
            addLine(lines, wrappedLines.get(index), fontSize, font, align,
                    index == wrappedLines.size() - 1 ? spacingAfter : 0F);
        }
    }

    private List<String> wrap(String text, int maxLineLength) {
        String normalized = defaultText(text, "-").replaceAll("\\s+", " ").trim();
        List<String> wrappedLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + word.length() + 1 <= maxLineLength) {
                currentLine.append(' ').append(word);
            } else {
                wrappedLines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }
        return wrappedLines.isEmpty() ? List.of("-") : wrappedLines;
    }

    private void addLine(List<PdfElement> lines, String text, int fontSize, PdfFont font, PdfAlign align, float spacingAfter) {
        lines.add(new PdfLine(defaultText(text, ""), fontSize, font, align, spacingAfter));
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
                + ". The following employees are mapped as part of this work order for execution, coordination, "
                + "and service delivery within the approved project scope.";
    }

    private String escapePdfText(String value) {
        return sanitizePdfText(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String sanitizePdfText(String value) {
        StringBuilder sanitized = new StringBuilder(defaultText(value, "").length());
        for (char character : defaultText(value, "").toCharArray()) {
            sanitized.append(character >= 32 && character <= 126 ? character : '?');
        }
        return sanitized.toString();
    }

    private String formatPdfNumber(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void writeAscii(ByteArrayOutputStream outputStream, String value) throws IOException {
        outputStream.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private String truncateForColumn(String value, float columnWidth, boolean header) {
        int maxLength = Math.max(3, (int) (columnWidth / (header ? 4.6F : 4.0F)));
        String normalized = defaultText(value, "-");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, Math.max(0, maxLength - 1)) + ".";
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

    private enum PdfFont {
        REGULAR("/F1"),
        BOLD("/F2"),
        MONO("/F3"),
        MONO_BOLD("/F4");

        private final String resourceName;

        PdfFont(String resourceName) {
            this.resourceName = resourceName;
        }

        private String resourceName() {
            return resourceName;
        }
    }

    private enum PdfAlign {
        LEFT,
        CENTER
    }

    private interface PdfElement {
        float height();

        float spacingAfter();
    }

    private record PdfLine(String text, int fontSize, PdfFont font, PdfAlign align, float spacingAfter) implements PdfElement {
        public float height() {
            return Math.max(DEFAULT_LINE_HEIGHT, fontSize + 3F);
        }
    }

    private record PdfTableRow(List<String> cells, boolean header, float spacingAfter) implements PdfElement {
        public float height() {
            return TABLE_ROW_HEIGHT;
        }
    }

    private record PositionedPdfElement(PdfElement element, float x, float y) {
    }

    private record PdfObject(int id, String content) {
    }
}
