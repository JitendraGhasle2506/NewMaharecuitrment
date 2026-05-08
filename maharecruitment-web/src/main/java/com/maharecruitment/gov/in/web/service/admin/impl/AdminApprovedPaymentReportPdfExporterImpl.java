package com.maharecruitment.gov.in.web.service.admin.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private static final float PAGE_WIDTH = 842f;
    private static final float PAGE_HEIGHT = 595f;
    private static final float LEFT_MARGIN = 24f;
    private static final float RIGHT_MARGIN = 24f;
    private static final float TOP_MARGIN = 22f;
    private static final float BOTTOM_MARGIN = 24f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;

    private static final float FIRST_PAGE_TABLE_TOP = 388f;
    private static final float CONTINUATION_TABLE_TOP = 520f;
    private static final float TABLE_HEADER_HEIGHT = 24f;
    private static final float ROW_LINE_HEIGHT = 8.5f;
    private static final float ROW_PADDING_TOP = 6f;
    private static final float ROW_PADDING_BOTTOM = 5f;
    private static final float MIN_ROW_HEIGHT = 22f;
    private static final float FOOTER_Y = 18f;

    private static final float[] COLUMN_WIDTHS = { 32f, 106f, 196f, 108f, 60f, 82f, 82f, 128f };
    private static final int[] COLUMN_CHAR_LIMITS = { 3, 18, 30, 18, 10, 13, 16, 20 };
    private static final String[] TABLE_HEADERS = {
            "Sr", "Request / Payment", "Department / Project", "Receipt Number",
            "Mode", "Amount", "Approved On", "Approved By"
    };

    @Override
    public byte[] export(AdminApprovedPaymentReportPdfData reportData) {
        List<RowLayout> rowLayouts = buildRowLayouts(reportData.rows());
        List<PageLayout> pages = paginate(rowLayouts);
        return buildPdfDocument(reportData, pages);
    }

    private List<RowLayout> buildRowLayouts(List<ApprovedPaymentReportRowView> rows) {
        List<RowLayout> layouts = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            List<List<String>> emptyCells = new ArrayList<>();
            emptyCells.add(List.of(""));
            emptyCells.add(List.of("No approved payment records found."));
            emptyCells.add(List.of("Adjust filters and generate again."));
            emptyCells.add(List.of(""));
            emptyCells.add(List.of(""));
            emptyCells.add(List.of(""));
            emptyCells.add(List.of(""));
            emptyCells.add(List.of(""));
            layouts.add(new RowLayout(emptyCells, 28f, false));
            return layouts;
        }

        int rowNumber = 1;
        for (ApprovedPaymentReportRowView row : rows) {
            List<List<String>> columns = new ArrayList<>();
            columns.add(wrapCell(List.of(String.valueOf(rowNumber)), COLUMN_CHAR_LIMITS[0]));
            columns.add(wrapCell(List.of(
                    safeOrDash(row.getRequestId()),
                    "Payment ID: " + safe(row.getPaymentId())), COLUMN_CHAR_LIMITS[1]));
            columns.add(wrapCell(List.of(
                    resolveDepartmentName(row),
                    compactProjectLabel(row)), COLUMN_CHAR_LIMITS[2]));
            columns.add(wrapCell(List.of(safeOrDash(row.getReceiptNumber())), COLUMN_CHAR_LIMITS[3]));
            columns.add(wrapCell(List.of(safeOrDash(row.getPaymentMode())), COLUMN_CHAR_LIMITS[4]));
            columns.add(wrapCell(List.of(formatAmount(row.getTotalAmount())), COLUMN_CHAR_LIMITS[5]));
            columns.add(wrapCell(List.of(formatDateTime(row.getApprovedDate())), COLUMN_CHAR_LIMITS[6]));
            columns.add(wrapCell(List.of(
                    safeOrDash(row.getApprovedBy()),
                    resolveStatus(row)), COLUMN_CHAR_LIMITS[7]));

            int maxLines = columns.stream()
                    .mapToInt(List::size)
                    .max()
                    .orElse(1);
            float height = Math.max(MIN_ROW_HEIGHT, ROW_PADDING_TOP + ROW_PADDING_BOTTOM + (maxLines * ROW_LINE_HEIGHT));
            layouts.add(new RowLayout(columns, height, true));
            rowNumber++;
        }
        return layouts;
    }

    private List<PageLayout> paginate(List<RowLayout> rows) {
        List<PageLayout> pages = new ArrayList<>();
        List<RowLayout> currentRows = new ArrayList<>();
        float remainingHeight = availableRowHeight(true);
        boolean firstPage = true;

        for (RowLayout row : rows) {
            if (!currentRows.isEmpty() && row.height() > remainingHeight) {
                pages.add(new PageLayout(firstPage, currentRows));
                firstPage = false;
                currentRows = new ArrayList<>();
                remainingHeight = availableRowHeight(false);
            }

            currentRows.add(row);
            remainingHeight -= row.height();
        }

        if (currentRows.isEmpty()) {
            currentRows.add(new RowLayout(List.of(
                    List.of(""),
                    List.of("No approved payment records found."),
                    List.of(""),
                    List.of(""),
                    List.of(""),
                    List.of(""),
                    List.of(""),
                    List.of("")), 28f, false));
        }

        pages.add(new PageLayout(firstPage, currentRows));
        return pages;
    }

    private float availableRowHeight(boolean firstPage) {
        float tableTop = firstPage ? FIRST_PAGE_TABLE_TOP : CONTINUATION_TABLE_TOP;
        return tableTop - BOTTOM_MARGIN - TABLE_HEADER_HEIGHT - 14f;
    }

    private byte[] buildPdfDocument(AdminApprovedPaymentReportPdfData reportData, List<PageLayout> pages) {
        List<byte[]> objects = new ArrayList<>();
        objects.add(pdfObject(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(pdfObject(2, "<< /Type /Pages /Kids [" + buildPageReferences(pages.size()) + "] /Count " + pages.size() + " >>"));
        objects.add(pdfObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(pdfObject(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));

        int objectNumber = 5;
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            int pageObjectNumber = objectNumber++;
            int contentObjectNumber = objectNumber++;

            objects.add(pdfObject(
                    pageObjectNumber,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + formatNumber(PAGE_WIDTH) + " " + formatNumber(PAGE_HEIGHT)
                            + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                            + contentObjectNumber + " 0 R >>"));

            byte[] streamBytes = buildContentStream(reportData, pages.get(pageIndex), pageIndex + 1, pages.size());
            objects.add(pdfStreamObject(contentObjectNumber, streamBytes));
        }

        return assemblePdf(objects);
    }

    private String buildPageReferences(int pageCount) {
        StringBuilder references = new StringBuilder();
        int pageObjectNumber = 5;
        for (int index = 0; index < pageCount; index++) {
            if (references.length() > 0) {
                references.append(' ');
            }
            references.append(pageObjectNumber).append(" 0 R");
            pageObjectNumber += 2;
        }
        return references.toString();
    }

    private byte[] buildContentStream(
            AdminApprovedPaymentReportPdfData reportData,
            PageLayout page,
            int pageNumber,
            int totalPages) {
        StringBuilder content = new StringBuilder();
        float tableTop;

        if (page.firstPage()) {
            appendFirstPageHeader(content, reportData, pageNumber, totalPages);
            tableTop = FIRST_PAGE_TABLE_TOP;
        } else {
            appendContinuationHeader(content, reportData, pageNumber, totalPages);
            tableTop = CONTINUATION_TABLE_TOP;
        }

        appendTable(content, tableTop, page.rows());
        appendFooter(content, reportData.generatedAt(), pageNumber, totalPages);
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private void appendFirstPageHeader(
            StringBuilder content,
            AdminApprovedPaymentReportPdfData reportData,
            int pageNumber,
            int totalPages) {
        float bannerY = PAGE_HEIGHT - TOP_MARGIN - 42f;
        fillRect(content, LEFT_MARGIN, bannerY, CONTENT_WIDTH, 42f, 0.91f, 0.95f, 0.99f);
        strokeRect(content, LEFT_MARGIN, bannerY, CONTENT_WIDTH, 42f, 0.41f, 0.52f, 0.66f, 1f);

        drawCenteredText(content, reportData.reportTitle(), PAGE_WIDTH / 2f, bannerY + 27f, "F2", 16f, 0.09f, 0.20f, 0.32f);
        drawCenteredText(content, "Administrative register of auditor-approved department payments", PAGE_WIDTH / 2f,
                bannerY + 11f, "F1", 9f, 0.29f, 0.35f, 0.43f);

        float metaY = bannerY - 70f;
        float metaGap = 10f;
        float metaWidth = (CONTENT_WIDTH - metaGap) / 2f;
        drawInfoCard(content, LEFT_MARGIN, metaY, metaWidth, 28f, "Department Scope", safeOrDash(reportData.departmentScope()));
        drawInfoCard(content, LEFT_MARGIN + metaWidth + metaGap, metaY, metaWidth, 28f, "Financial Year",
                safeOrDash(reportData.financialYearLabel()));

        float metaSecondRowY = metaY - 34f;
        drawInfoCard(content, LEFT_MARGIN, metaSecondRowY, metaWidth, 28f, "Approved Date Range",
                safeOrDash(reportData.approvedDateRangeLabel()));
        drawInfoCard(content, LEFT_MARGIN + metaWidth + metaGap, metaSecondRowY, metaWidth, 28f, "Generated On",
                formatDateTime(reportData.generatedAt()));

        float summaryY = metaSecondRowY - 48f;
        ApprovedPaymentReportSummaryView summary = reportData.summary();
        float summaryGap = 12f;
        float summaryWidth = (CONTENT_WIDTH - (summaryGap * 2f)) / 3f;
        drawSummaryCard(content, LEFT_MARGIN, summaryY, summaryWidth, 36f, "Approved Payments",
                String.valueOf(summary == null ? 0L : summary.getTotalPayments()));
        drawSummaryCard(content, LEFT_MARGIN + summaryWidth + summaryGap, summaryY, summaryWidth, 36f, "Approved Amount",
                formatAmount(summary == null ? BigDecimal.ZERO : summary.getTotalApprovedAmount()));
        drawSummaryCard(content, LEFT_MARGIN + ((summaryWidth + summaryGap) * 2f), summaryY, summaryWidth, 36f,
                "Departments Covered",
                String.valueOf(summary == null ? 0L : summary.getTotalDepartments()));

        drawText(content, "Page " + pageNumber + " of " + totalPages, PAGE_WIDTH - RIGHT_MARGIN - 52f, summaryY + 14f,
                "F1", 8f, 0.35f, 0.40f, 0.46f);
    }

    private void appendContinuationHeader(
            StringBuilder content,
            AdminApprovedPaymentReportPdfData reportData,
            int pageNumber,
            int totalPages) {
        float topY = PAGE_HEIGHT - TOP_MARGIN - 20f;
        drawText(content, reportData.reportTitle(), LEFT_MARGIN, topY, "F2", 13f, 0.09f, 0.20f, 0.32f);
        drawText(content, "Generated On: " + formatDateTime(reportData.generatedAt()), LEFT_MARGIN, topY - 14f,
                "F1", 8f, 0.35f, 0.40f, 0.46f);
        drawText(content, "Page " + pageNumber + " of " + totalPages, PAGE_WIDTH - RIGHT_MARGIN - 60f, topY,
                "F1", 8f, 0.35f, 0.40f, 0.46f);
        drawLine(content, LEFT_MARGIN, topY - 22f, PAGE_WIDTH - RIGHT_MARGIN, topY - 22f, 0.75f, 0.81f, 0.88f, 1f);
    }

    private void appendTable(StringBuilder content, float tableTop, List<RowLayout> rows) {
        float tableX = LEFT_MARGIN;
        float currentY = tableTop;

        fillRect(content, tableX, currentY - TABLE_HEADER_HEIGHT, CONTENT_WIDTH, TABLE_HEADER_HEIGHT, 0.90f, 0.94f, 0.97f);
        strokeRect(content, tableX, currentY - TABLE_HEADER_HEIGHT, CONTENT_WIDTH, TABLE_HEADER_HEIGHT, 0.66f, 0.73f, 0.80f, 1f);

        float currentX = tableX;
        for (int columnIndex = 0; columnIndex < COLUMN_WIDTHS.length; columnIndex++) {
            if (columnIndex > 0) {
                drawLine(content, currentX, currentY - TABLE_HEADER_HEIGHT, currentX, currentY, 0.66f, 0.73f, 0.80f, 1f);
            }
            drawText(content, TABLE_HEADERS[columnIndex], currentX + 3f, currentY - 16f, "F2", 7.5f, 0.09f, 0.20f, 0.32f);
            currentX += COLUMN_WIDTHS[columnIndex];
        }

        currentY -= TABLE_HEADER_HEIGHT;
        boolean alternate = false;
        for (RowLayout row : rows) {
            if (alternate && row.hasData()) {
                fillRect(content, tableX, currentY - row.height(), CONTENT_WIDTH, row.height(), 0.985f, 0.99f, 1f);
            }
            strokeRect(content, tableX, currentY - row.height(), CONTENT_WIDTH, row.height(), 0.84f, 0.88f, 0.92f, 0.8f);

            currentX = tableX;
            for (int columnIndex = 0; columnIndex < COLUMN_WIDTHS.length; columnIndex++) {
                if (columnIndex > 0) {
                    drawLine(content, currentX, currentY - row.height(), currentX, currentY, 0.84f, 0.88f, 0.92f, 0.8f);
                }
                drawCellText(content, currentX + 3f, currentY - ROW_PADDING_TOP - 7f, row.columns().get(columnIndex),
                        row.height(), columnIndex == 0 ? "F2" : "F1", 7.2f,
                        columnIndex == 5 ? 0.05f : 0.18f, columnIndex == 5 ? 0.36f : 0.23f,
                        columnIndex == 5 ? 0.20f : 0.29f);
                currentX += COLUMN_WIDTHS[columnIndex];
            }

            currentY -= row.height();
            alternate = !alternate;
        }
    }

    private void appendFooter(StringBuilder content, LocalDateTime generatedAt, int pageNumber, int totalPages) {
        drawLine(content, LEFT_MARGIN, FOOTER_Y + 10f, PAGE_WIDTH - RIGHT_MARGIN, FOOTER_Y + 10f, 0.75f, 0.81f, 0.88f, 1f);
        drawText(content, "Approved payment report generated for administrative use.", LEFT_MARGIN, FOOTER_Y,
                "F1", 7.5f, 0.40f, 0.45f, 0.51f);
        drawText(content, "Generated: " + formatDateTime(generatedAt), PAGE_WIDTH - RIGHT_MARGIN - 180f, FOOTER_Y,
                "F1", 7.5f, 0.40f, 0.45f, 0.51f);
        drawText(content, pageNumber + "/" + totalPages, PAGE_WIDTH - RIGHT_MARGIN - 18f, FOOTER_Y,
                "F1", 7.5f, 0.40f, 0.45f, 0.51f);
    }

    private void drawInfoCard(
            StringBuilder content,
            float x,
            float y,
            float width,
            float height,
            String label,
            String value) {
        fillRect(content, x, y, width, height, 0.98f, 0.99f, 1f);
        strokeRect(content, x, y, width, height, 0.82f, 0.87f, 0.92f, 1f);
        drawText(content, label, x + 8f, y + height - 11f, "F2", 7.5f, 0.41f, 0.47f, 0.54f);
        drawText(content, truncate(value, 52), x + 8f, y + 9f, "F1", 8.5f, 0.09f, 0.20f, 0.32f);
    }

    private void drawSummaryCard(
            StringBuilder content,
            float x,
            float y,
            float width,
            float height,
            String label,
            String value) {
        fillRect(content, x, y, width, height, 0.95f, 0.98f, 0.96f);
        strokeRect(content, x, y, width, height, 0.75f, 0.84f, 0.78f, 1f);
        drawText(content, label, x + 8f, y + height - 11f, "F2", 7.5f, 0.28f, 0.39f, 0.31f);
        drawText(content, truncate(value, 28), x + 8f, y + 10f, "F2", 10f, 0.05f, 0.32f, 0.18f);
    }

    private void drawCellText(
            StringBuilder content,
            float x,
            float baselineY,
            List<String> lines,
            float rowHeight,
            String fontKey,
            float fontSize,
            float red,
            float green,
            float blue) {
        float y = baselineY;
        float minY = baselineY - rowHeight + ROW_PADDING_BOTTOM + 2f;
        for (String line : lines) {
            if (y < minY) {
                break;
            }
            drawText(content, line, x, y, fontKey, fontSize, red, green, blue);
            y -= ROW_LINE_HEIGHT;
        }
    }

    private void fillRect(StringBuilder content, float x, float y, float width, float height, float red, float green, float blue) {
        content.append("q ")
                .append(formatNumber(red)).append(' ')
                .append(formatNumber(green)).append(' ')
                .append(formatNumber(blue)).append(" rg ")
                .append(formatNumber(x)).append(' ')
                .append(formatNumber(y)).append(' ')
                .append(formatNumber(width)).append(' ')
                .append(formatNumber(height)).append(" re f Q\n");
    }

    private void strokeRect(
            StringBuilder content,
            float x,
            float y,
            float width,
            float height,
            float red,
            float green,
            float blue,
            float lineWidth) {
        content.append("q ")
                .append(formatNumber(lineWidth)).append(" w ")
                .append(formatNumber(red)).append(' ')
                .append(formatNumber(green)).append(' ')
                .append(formatNumber(blue)).append(" RG ")
                .append(formatNumber(x)).append(' ')
                .append(formatNumber(y)).append(' ')
                .append(formatNumber(width)).append(' ')
                .append(formatNumber(height)).append(" re S Q\n");
    }

    private void drawLine(
            StringBuilder content,
            float startX,
            float startY,
            float endX,
            float endY,
            float red,
            float green,
            float blue,
            float lineWidth) {
        content.append("q ")
                .append(formatNumber(lineWidth)).append(" w ")
                .append(formatNumber(red)).append(' ')
                .append(formatNumber(green)).append(' ')
                .append(formatNumber(blue)).append(" RG ")
                .append(formatNumber(startX)).append(' ')
                .append(formatNumber(startY)).append(" m ")
                .append(formatNumber(endX)).append(' ')
                .append(formatNumber(endY)).append(" l S Q\n");
    }

    private void drawText(
            StringBuilder content,
            String text,
            float x,
            float y,
            String fontKey,
            float fontSize,
            float red,
            float green,
            float blue) {
        content.append("BT ")
                .append(formatNumber(red)).append(' ')
                .append(formatNumber(green)).append(' ')
                .append(formatNumber(blue)).append(" rg /")
                .append(fontKey).append(' ')
                .append(formatNumber(fontSize)).append(" Tf 1 0 0 1 ")
                .append(formatNumber(x)).append(' ')
                .append(formatNumber(y)).append(" Tm (")
                .append(escapePdfText(text))
                .append(") Tj ET\n");
    }

    private void drawCenteredText(
            StringBuilder content,
            String text,
            float centerX,
            float y,
            String fontKey,
            float fontSize,
            float red,
            float green,
            float blue) {
        float approximateWidth = text.length() * (fontSize * 0.33f);
        drawText(content, text, centerX - (approximateWidth / 2f), y, fontKey, fontSize, red, green, blue);
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

            outputStream.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n")
                    .getBytes(StandardCharsets.US_ASCII));
            outputStream.write(("startxref\n" + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
            return outputStream.toByteArray();
        } catch (IOException exception) {
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
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate approved payment report PDF.", exception);
        }
    }

    private List<String> wrapCell(List<String> values, int maxCharacters) {
        List<String> lines = new ArrayList<>();
        for (String value : values) {
            String safeValue = safeOrDash(value);
            lines.addAll(wrapText(safeValue, maxCharacters));
        }
        return lines.isEmpty() ? List.of("-") : lines;
    }

    private List<String> wrapText(String value, int maxCharacters) {
        List<String> wrapped = new ArrayList<>();
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            wrapped.add("-");
            return wrapped;
        }

        String[] words = normalized.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.length() > maxCharacters) {
                if (line.length() > 0) {
                    wrapped.add(line.toString());
                    line.setLength(0);
                }
                int start = 0;
                while (start < word.length()) {
                    int end = Math.min(start + maxCharacters, word.length());
                    wrapped.add(word.substring(start, end));
                    start = end;
                }
                continue;
            }

            if (line.length() == 0) {
                line.append(word);
                continue;
            }

            if (line.length() + 1 + word.length() <= maxCharacters) {
                line.append(' ').append(word);
            } else {
                wrapped.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {
            wrapped.add(line.toString());
        }
        return wrapped;
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

    private String resolveStatus(ApprovedPaymentReportRowView row) {
        if (row == null || row.getApplicationStatus() == null || !hasText(row.getApplicationStatus().getDisplayName())) {
            return "Status: -";
        }
        return "Status: " + row.getApplicationStatus().getDisplayName();
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return "INR " + AMOUNT_FORMAT.format(safeAmount);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String formatNumber(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String escapePdfText(String value) {
        String ascii = safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < ascii.length(); index++) {
            char character = ascii.charAt(index);
            sanitized.append(character <= 127 ? character : '?');
        }
        return sanitized.toString();
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

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String safeOrDash(Object value) {
        String text = safe(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record RowLayout(
            List<List<String>> columns,
            float height,
            boolean hasData) {
    }

    private record PageLayout(
            boolean firstPage,
            List<RowLayout> rows) {
    }
}
