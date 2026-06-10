package com.maharecruitment.gov.in.web.service.hr.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto;

@Service
public class AgencyEmployeeRelievingReportPdfGenerator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", Locale.ENGLISH);
    private static final float PAGE_WIDTH = 842F;
    private static final float PAGE_HEIGHT = 595F;
    private static final float LEFT_MARGIN = 30F;
    private static final float TOP_MARGIN = 34F;
    private static final float BOTTOM_MARGIN = 52F;
    private static final float FOOTER_LINE_Y = 28F;
    private static final float FOOTER_TEXT_Y = 16F;
    private static final float TABLE_ROW_HEIGHT = 18F;
    private static final float TABLE_TEXT_TOP_PADDING = 12F;
    private static final float TABLE_CELL_PADDING = 4F;
    
    // Total width available = 842 - 60 = 782
    private static final float[] COLUMN_WIDTHS = {100F, 282F, 120F, 120F, 160F};

    public byte[] generate(List<EmployeeRelievingDto> records, String agencyName) {
        try {
            return writePdf(records, agencyName);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate relieving report PDF.", ex);
        }
    }

    private byte[] writePdf(List<EmployeeRelievingDto> records, String agencyName) throws IOException {
        List<PdfPage> pages = paginate(records, agencyName);
        List<PdfObject> objects = new ArrayList<>();

        StringBuilder kids = new StringBuilder();
        int nextObjectId = 7;
        for (PdfPage page : pages) {
            int contentObjectId = nextObjectId++;
            int pageObjectId = nextObjectId++;
            kids.append(pageObjectId).append(" 0 R ");

            byte[] content = buildPageContent(page);
            objects.add(new PdfObject(contentObjectId, "<< /Length " + content.length + " >>\nstream\n"
                    + new String(content, StandardCharsets.US_ASCII) + "endstream"));
            objects.add(new PdfObject(pageObjectId, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH
                    + " " + PAGE_HEIGHT + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                    + contentObjectId + " 0 R >>"));
        }

        objects.add(0, new PdfObject(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));
        objects.add(0, new PdfObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(0, new PdfObject(2, "<< /Type /Pages /Kids [" + kids.toString().trim()
                + "] /Count " + pages.size() + " >>"));
        objects.add(0, new PdfObject(1, "<< /Type /Catalog /Pages 2 0 R >>"));

        return serializePdf(objects, nextObjectId - 1);
    }

    private List<PdfPage> paginate(List<EmployeeRelievingDto> records, String agencyName) {
        List<PdfPage> pages = new ArrayList<>();
        PdfPage page = new PdfPage();
        float y = PAGE_HEIGHT - TOP_MARGIN;

        y = appendPageHeading(page, agencyName, y);
        y -= 6F;
        appendTableHeader(page, y);
        y -= TABLE_ROW_HEIGHT;

        if (records == null || records.isEmpty()) {
            appendText(page, "No relieving records available.", LEFT_MARGIN, y, 10, PdfFont.REGULAR);
            pages.add(page);
            return finalizePages(pages);
        }

        for (EmployeeRelievingDto row : records) {
            if (y - TABLE_ROW_HEIGHT < BOTTOM_MARGIN) {
                pages.add(page);
                page = new PdfPage();
                y = PAGE_HEIGHT - TOP_MARGIN;
                y = appendPageHeading(page, agencyName, y);
                appendTableHeader(page, y);
                y -= TABLE_ROW_HEIGHT;
            }

            appendTableRow(page, row, y);
            y -= TABLE_ROW_HEIGHT;
        }

        pages.add(page);
        return finalizePages(pages);
    }

    private List<PdfPage> finalizePages(List<PdfPage> pages) {
        int totalPages = pages.size();
        for (int index = 0; index < totalPages; index++) {
            PdfPage page = pages.get(index);
            page.pageNumber = index + 1;
            page.totalPages = totalPages;
            appendPageFooter(page);
        }
        return pages;
    }

    private float appendPageHeading(PdfPage page, String agencyName, float y) {
        appendText(page, "EMPLOYEE RELIEVING REPORT", centerX("EMPLOYEE RELIEVING REPORT", 15),
                y, 15, PdfFont.BOLD);
        y -= 18F;
        
        String agencyHeader = "Agency: " + defaultText(agencyName, "-");
        appendText(page, agencyHeader, centerX(agencyHeader, 10), y, 10, PdfFont.BOLD);
        y -= 12F;
        
        appendHorizontalRule(page, LEFT_MARGIN, y - 6F, PAGE_WIDTH - LEFT_MARGIN, 0.8F);
        return y - 12F;
    }

    private void appendPageFooter(PdfPage page) {
        appendHorizontalRule(page, LEFT_MARGIN, FOOTER_LINE_Y, PAGE_WIDTH - LEFT_MARGIN, 0.8F);
        appendText(page,
                "Generated On: " + formatTimestamp(LocalDateTime.now()),
                LEFT_MARGIN,
                FOOTER_TEXT_Y,
                8,
                PdfFont.REGULAR);
        appendText(page,
                "This report is system generated.",
                centerX("This report is system generated.", 8),
                FOOTER_TEXT_Y,
                8,
                PdfFont.REGULAR);
        appendText(page,
                "Page " + page.pageNumber + " of " + page.totalPages,
                rightAlignedX("Page " + page.pageNumber + " of " + page.totalPages, 8, PAGE_WIDTH - LEFT_MARGIN),
                FOOTER_TEXT_Y,
                8,
                PdfFont.REGULAR);
    }

    private void appendTableHeader(PdfPage page, float y) {
        List<String> cells = new ArrayList<>();
        cells.add("Employee ID");
        cells.add("Employee Name");
        cells.add("Resignation Date");
        cells.add("Exit Date");
        cells.add("Current Status");
        page.rows.add(new PdfTableRow(cells, true, y));
    }

    private void appendTableRow(PdfPage page, EmployeeRelievingDto row, float y) {
        List<String> cells = new ArrayList<>();
        cells.add(truncateForColumn(defaultText(row.getEmployeeCode(), "N/A"), COLUMN_WIDTHS[0]));
        cells.add(truncateForColumn(defaultText(row.getEmployeeName(), ""), COLUMN_WIDTHS[1]));
        cells.add(row.getResignDate() != null ? row.getResignDate().toString() : "-");
        cells.add(row.getExitDate() != null ? row.getExitDate().toString() : "-");
        cells.add(truncateForColumn(defaultText(row.getStatus(), ""), COLUMN_WIDTHS[4]));
        page.rows.add(new PdfTableRow(cells, false, y));
    }

    private byte[] buildPageContent(PdfPage page) {
        StringBuilder content = new StringBuilder();
        for (String drawing : page.drawings) {
            content.append(drawing);
        }
        for (PdfText text : page.texts) {
            appendText(content, text.value(), text.x(), text.y(), text.fontSize(), text.font());
        }
        for (PdfTableRow row : page.rows) {
            appendTableRow(content, row);
        }
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private void appendTableRow(StringBuilder content, PdfTableRow row) {
        float x = LEFT_MARGIN;
        float y = row.y();
        float tableWidth = 0F;
        for (float columnWidth : COLUMN_WIDTHS) {
            tableWidth += columnWidth;
        }

        if (row.header()) {
            content.append("0.92 g\n");
            content.append(formatPdfNumber(x)).append(' ')
                    .append(formatPdfNumber(y - TABLE_ROW_HEIGHT)).append(' ')
                    .append(formatPdfNumber(tableWidth)).append(' ')
                    .append(formatPdfNumber(TABLE_ROW_HEIGHT)).append(" re f\n");
            content.append("0 g\n");
        }

        content.append("0.5 w\n");
        content.append(formatPdfNumber(x)).append(' ')
                .append(formatPdfNumber(y - TABLE_ROW_HEIGHT)).append(' ')
                .append(formatPdfNumber(tableWidth)).append(' ')
                .append(formatPdfNumber(TABLE_ROW_HEIGHT)).append(" re S\n");

        float nextX = x;
        for (int index = 0; index < COLUMN_WIDTHS.length - 1; index++) {
            nextX += COLUMN_WIDTHS[index];
            content.append(formatPdfNumber(nextX)).append(' ')
                    .append(formatPdfNumber(y - TABLE_ROW_HEIGHT)).append(" m ")
                    .append(formatPdfNumber(nextX)).append(' ')
                    .append(formatPdfNumber(y)).append(" l S\n");
        }

        float cellX = x;
        for (int index = 0; index < row.cells().size() && index < COLUMN_WIDTHS.length; index++) {
            appendText(
                    content,
                    row.cells().get(index),
                    cellX + TABLE_CELL_PADDING,
                    y - TABLE_TEXT_TOP_PADDING,
                    row.header() ? 9 : 8,
                    row.header() ? PdfFont.BOLD : PdfFont.REGULAR);
            cellX += COLUMN_WIDTHS[index];
        }
    }

    private void appendText(PdfPage page, String text, float x, float y, int fontSize, PdfFont font) {
        page.texts.add(new PdfText(defaultText(text, ""), x, y, fontSize, font));
    }

    private void appendHorizontalRule(PdfPage page, float startX, float y, float endX, float lineWidth) {
        page.drawings.add(formatPdfNumber(lineWidth) + " w\n"
                + "0.55 G\n"
                + formatPdfNumber(startX) + " " + formatPdfNumber(y) + " m "
                + formatPdfNumber(endX) + " " + formatPdfNumber(y) + " l S\n"
                + "0 G\n");
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

    private float centerX(String text, int fontSize) {
        float estimatedWidth = sanitizePdfText(text).length() * fontSize * 0.42F;
        return Math.max(LEFT_MARGIN, (PAGE_WIDTH - estimatedWidth) / 2F);
    }

    private float rightAlignedX(String text, int fontSize, float rightEdge) {
        float estimatedWidth = sanitizePdfText(text).length() * fontSize * 0.42F;
        return Math.max(LEFT_MARGIN, rightEdge - estimatedWidth);
    }

    private String truncateForColumn(String value, float columnWidth) {
        int maxLength = Math.max(2, (int) (columnWidth / 4.5F)); // Adjust character width heuristic
        return truncate(value, maxLength);
    }

    private String truncate(String value, int maxLength) {
        String normalized = defaultText(value, "");
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, Math.max(0, maxLength - 1)) + ".";
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

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatPdfNumber(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatTimestamp(LocalDateTime generatedAt) {
        return generatedAt == null ? "-" : TIMESTAMP_FORMAT.format(generatedAt).toLowerCase(Locale.ENGLISH);
    }

    private void writeAscii(ByteArrayOutputStream outputStream, String value) throws IOException {
        outputStream.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private enum PdfFont {
        REGULAR("/F1"),
        BOLD("/F2");

        private final String resourceName;

        PdfFont(String resourceName) {
            this.resourceName = resourceName;
        }

        private String resourceName() {
            return resourceName;
        }
    }

    private static final class PdfPage {
        private final List<String> drawings = new ArrayList<>();
        private final List<PdfText> texts = new ArrayList<>();
        private final List<PdfTableRow> rows = new ArrayList<>();
        private int pageNumber;
        private int totalPages;
    }

    private record PdfText(String value, float x, float y, int fontSize, PdfFont font) {
    }

    private record PdfTableRow(List<String> cells, boolean header, float y) {
    }

    private record PdfObject(int id, String content) {
    }
}
