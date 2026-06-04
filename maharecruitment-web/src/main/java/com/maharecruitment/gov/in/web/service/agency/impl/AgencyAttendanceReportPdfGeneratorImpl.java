package com.maharecruitment.gov.in.web.service.agency.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.service.model.GeneratedAttendanceReportDocument;
import com.maharecruitment.gov.in.web.service.agency.AgencyAttendanceReportPdfGenerator;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportFilter;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportPageView;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportRow;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportSummary;

@Service
public class AgencyAttendanceReportPdfGeneratorImpl implements AgencyAttendanceReportPdfGenerator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter GENERATED_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm",
            Locale.ENGLISH);

    private static final float PAGE_WIDTH = 1191f;
    private static final float PAGE_HEIGHT = 842f;
    private static final float LEFT_MARGIN = 28f;
    private static final float RIGHT_MARGIN = 28f;
    private static final float TOP_MARGIN = 26f;
    private static final float BOTTOM_MARGIN = 38f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;
    private static final float FIRST_PAGE_TABLE_TOP = 628f;
    private static final float CONTINUATION_TABLE_TOP = 760f;
    private static final float TABLE_HEADER_HEIGHT = 18f;
    private static final float TABLE_ROW_HEIGHT = 18f;
    private static final float FOOTER_Y = 18f;

    private static final float TYPE_COLUMN_WIDTH = 46f;
    private static final float REQUEST_COLUMN_WIDTH = 70f;
    private static final float EMPLOYEE_COLUMN_WIDTH = 160f;
    private static final float PROJECT_COLUMN_WIDTH = 110f;
    private static final float TOTAL_COLUMN_WIDTH = 24f;
    private static final int TOTAL_COLUMN_COUNT = 8;

    private static final String[] TOTAL_HEADERS = { "P", "A+L", "L", "CO", "T", "H", "W", "Pay" };

    @Override
    public GeneratedAttendanceReportDocument generate(AgencyAttendanceReportPageView report) {
        Objects.requireNonNull(report, "report");
        PdfReportContext context = new PdfReportContext(
                report,
                LocalDateTime.now(),
                normalizeDaysInMonth(report.daysInMonth()),
                buildColumnWidths(report.daysInMonth()));
        byte[] bytes = buildPdfDocument(context, paginate(context));
        return new GeneratedAttendanceReportDocument(
                buildFileName(context),
                PDF_CONTENT_TYPE,
                bytes,
                bytes.length);
    }

    private List<PageLayout> paginate(PdfReportContext context) {
        List<AgencyAttendanceReportRow> rows = safeRows(context.report());
        if (rows.isEmpty()) {
            return List.of(new PageLayout(true, List.of()));
        }

        List<PageLayout> pages = new ArrayList<>();
        int rowIndex = 0;
        boolean firstPage = true;
        while (rowIndex < rows.size()) {
            int capacity = rowCapacity(firstPage ? FIRST_PAGE_TABLE_TOP : CONTINUATION_TABLE_TOP);
            int toIndex = Math.min(rows.size(), rowIndex + capacity);
            pages.add(new PageLayout(firstPage, rows.subList(rowIndex, toIndex)));
            rowIndex = toIndex;
            firstPage = false;
        }
        return pages;
    }

    private byte[] buildPdfDocument(PdfReportContext context, List<PageLayout> pages) {
        List<byte[]> objects = new ArrayList<>();
        objects.add(pdfObject(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(pdfObject(2, "<< /Type /Pages /Kids [" + buildPageReferences(pages.size()) + "] /Count "
                + pages.size() + " >>"));
        objects.add(pdfObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(pdfObject(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));

        int objectNumber = 5;
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            int pageObjectNumber = objectNumber++;
            int contentObjectNumber = objectNumber++;
            objects.add(pdfObject(pageObjectNumber,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + formatNumber(PAGE_WIDTH) + " "
                            + formatNumber(PAGE_HEIGHT)
                            + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                            + contentObjectNumber + " 0 R >>"));
            objects.add(pdfStreamObject(contentObjectNumber,
                    buildContentStream(context, pages.get(pageIndex), pageIndex + 1, pages.size())));
        }

        return assemblePdf(objects);
    }

    private byte[] buildContentStream(
            PdfReportContext context,
            PageLayout page,
            int pageNumber,
            int totalPages) {
        StringBuilder content = new StringBuilder(16_384);
        float tableTop;
        if (page.firstPage()) {
            appendFirstPageHeader(content, context, pageNumber, totalPages);
            tableTop = FIRST_PAGE_TABLE_TOP;
        } else {
            appendContinuationHeader(content, context, pageNumber, totalPages);
            tableTop = CONTINUATION_TABLE_TOP;
        }

        appendTable(content, context, tableTop, page.rows());
        appendFooter(content, context.generatedAt(), pageNumber, totalPages);
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private void appendFirstPageHeader(
            StringBuilder content,
            PdfReportContext context,
            int pageNumber,
            int totalPages) {
        AgencyAttendanceReportPageView report = context.report();
        AgencyAttendanceReportFilter filter = report.filter();
        AgencyAttendanceReportSummary summary = safeSummary(report.summary());

        float bannerY = PAGE_HEIGHT - TOP_MARGIN - 48f;
        fillRect(content, LEFT_MARGIN, bannerY, CONTENT_WIDTH, 48f, 0.08f, 0.23f, 0.31f);
        strokeRect(content, LEFT_MARGIN, bannerY, CONTENT_WIDTH, 48f, 0.12f, 0.35f, 0.44f, 1f);
        drawText(content, "Agency Attendance Report", LEFT_MARGIN + 14f, bannerY + 29f, "F2", 16f,
                1f, 1f, 1f);
        drawText(content, safeOrDash(report.agencyName()) + " | " + periodLabel(report), LEFT_MARGIN + 14f,
                bannerY + 13f, "F1", 9f, 0.86f, 0.94f, 0.97f);
        drawText(content, "Page " + pageNumber + " of " + totalPages, PAGE_WIDTH - RIGHT_MARGIN - 72f,
                bannerY + 29f, "F1", 8f, 0.86f, 0.94f, 0.97f);
        drawText(content, "Generated: " + GENERATED_FORMATTER.format(context.generatedAt()),
                PAGE_WIDTH - RIGHT_MARGIN - 148f, bannerY + 13f, "F1", 8f, 0.86f, 0.94f, 0.97f);

        float metaY = bannerY - 42f;
        float metaGap = 8f;
        float metaWidth = (CONTENT_WIDTH - (metaGap * 3f)) / 4f;
        drawInfoCard(content, LEFT_MARGIN, metaY, metaWidth, 32f, "Agency", safeOrDash(report.agencyName()));
        drawInfoCard(content, LEFT_MARGIN + metaWidth + metaGap, metaY, metaWidth, 32f, "Period",
                periodLabel(report));
        drawInfoCard(content, LEFT_MARGIN + ((metaWidth + metaGap) * 2f), metaY, metaWidth, 32f,
                "Employee Type", formatEmployeeType(filter == null ? null : filter.employeeType()));
        drawInfoCard(content, LEFT_MARGIN + ((metaWidth + metaGap) * 3f), metaY, metaWidth, 32f, "Search",
                searchLabel(filter == null ? null : filter.searchText()));

        float summaryY = metaY - 42f;
        float summaryGap = 8f;
        float summaryWidth = (CONTENT_WIDTH - (summaryGap * 5f)) / 6f;
        drawSummaryCard(content, LEFT_MARGIN, summaryY, summaryWidth, 34f, "Employees",
                Integer.toString(summary.employeeCount()));
        drawSummaryCard(content, LEFT_MARGIN + summaryWidth + summaryGap, summaryY, summaryWidth, 34f, "Internal",
                Integer.toString(summary.internalEmployeeCount()));
        drawSummaryCard(content, LEFT_MARGIN + ((summaryWidth + summaryGap) * 2f), summaryY, summaryWidth, 34f,
                "External", Integer.toString(summary.externalEmployeeCount()));
        drawSummaryCard(content, LEFT_MARGIN + ((summaryWidth + summaryGap) * 3f), summaryY, summaryWidth, 34f,
                "Present", Long.toString(summary.presentDays()));
        drawSummaryCard(content, LEFT_MARGIN + ((summaryWidth + summaryGap) * 4f), summaryY, summaryWidth, 34f,
                "Absent + Leave", Long.toString(summary.absentDays() + summary.leaveDays()));
        drawSummaryCard(content, LEFT_MARGIN + ((summaryWidth + summaryGap) * 5f), summaryY, summaryWidth, 34f,
                "Payable", Long.toString(summary.payableDays()));

        drawText(content, "Legend: P Present | A Absent | L Leave | CO Comp Off | T Tour | H Holiday | W Week Off",
                LEFT_MARGIN, summaryY - 18f, "F1", 8f, 0.32f, 0.39f, 0.46f);
    }

    private void appendContinuationHeader(
            StringBuilder content,
            PdfReportContext context,
            int pageNumber,
            int totalPages) {
        float titleY = PAGE_HEIGHT - TOP_MARGIN - 18f;
        drawText(content, "Agency Attendance Report", LEFT_MARGIN, titleY, "F2", 13f, 0.09f, 0.20f, 0.30f);
        drawText(content, safeOrDash(context.report().agencyName()) + " | " + periodLabel(context.report()),
                LEFT_MARGIN, titleY - 14f, "F1", 8f, 0.35f, 0.42f, 0.49f);
        drawText(content, "Page " + pageNumber + " of " + totalPages, PAGE_WIDTH - RIGHT_MARGIN - 72f,
                titleY, "F1", 8f, 0.35f, 0.42f, 0.49f);
        drawLine(content, LEFT_MARGIN, titleY - 22f, PAGE_WIDTH - RIGHT_MARGIN, titleY - 22f,
                0.74f, 0.80f, 0.86f, 1f);
    }

    private void appendTable(
            StringBuilder content,
            PdfReportContext context,
            float tableTop,
            List<AgencyAttendanceReportRow> rows) {
        float[] widths = context.columnWidths();
        float tableWidth = sum(widths);
        float currentY = tableTop;

        fillRect(content, LEFT_MARGIN, currentY - TABLE_HEADER_HEIGHT, tableWidth, TABLE_HEADER_HEIGHT,
                0.90f, 0.94f, 0.97f);
        strokeRect(content, LEFT_MARGIN, currentY - TABLE_HEADER_HEIGHT, tableWidth, TABLE_HEADER_HEIGHT,
                0.63f, 0.71f, 0.79f, 1f);
        appendHeaderCells(content, context.daysInMonth(), widths, currentY);
        currentY -= TABLE_HEADER_HEIGHT;

        if (rows == null || rows.isEmpty()) {
            strokeRect(content, LEFT_MARGIN, currentY - 28f, tableWidth, 28f, 0.82f, 0.87f, 0.92f, 1f);
            drawText(content, "No attendance records found for the selected filters.", LEFT_MARGIN + 8f,
                    currentY - 17f, "F1", 8f, 0.30f, 0.36f, 0.43f);
            return;
        }

        boolean alternate = false;
        for (AgencyAttendanceReportRow row : rows) {
            if (alternate) {
                fillRect(content, LEFT_MARGIN, currentY - TABLE_ROW_HEIGHT, tableWidth, TABLE_ROW_HEIGHT,
                        0.985f, 0.99f, 1f);
            }
            strokeRect(content, LEFT_MARGIN, currentY - TABLE_ROW_HEIGHT, tableWidth, TABLE_ROW_HEIGHT,
                    0.84f, 0.88f, 0.92f, 0.8f);
            appendBodyCells(content, context, row, currentY);
            currentY -= TABLE_ROW_HEIGHT;
            alternate = !alternate;
        }
    }

    private void appendHeaderCells(StringBuilder content, int daysInMonth, float[] widths, float y) {
        List<String> headers = new ArrayList<>();
        headers.add("Type");
        headers.add("Request");
        headers.add("Employee");
        headers.add("Project");
        for (int day = 1; day <= daysInMonth; day++) {
            headers.add(String.format(Locale.ROOT, "%02d", day));
        }
        headers.addAll(List.of(TOTAL_HEADERS));

        float x = LEFT_MARGIN;
        for (int index = 0; index < widths.length; index++) {
            if (index > 0) {
                drawLine(content, x, y - TABLE_HEADER_HEIGHT, x, y, 0.63f, 0.71f, 0.79f, 0.8f);
            }
            String header = index < headers.size() ? headers.get(index) : "";
            drawCellText(content, header, x, y, widths[index], TABLE_HEADER_HEIGHT, "F2", 5.8f,
                    0.10f, 0.20f, 0.31f, index >= 4);
            x += widths[index];
        }
    }

    private void appendBodyCells(
            StringBuilder content,
            PdfReportContext context,
            AgencyAttendanceReportRow row,
            float y) {
        float[] widths = context.columnWidths();
        List<CellValue> cells = buildBodyCells(row, context.daysInMonth());
        float x = LEFT_MARGIN;
        for (int index = 0; index < widths.length; index++) {
            if (index > 0) {
                drawLine(content, x, y - TABLE_ROW_HEIGHT, x, y, 0.84f, 0.88f, 0.92f, 0.7f);
            }
            CellValue cell = index < cells.size() ? cells.get(index) : new CellValue("", false);
            if (cell.statusCell()) {
                StatusColor statusColor = statusColor(cell.value());
                if (statusColor != null) {
                    fillRect(content, x + 1.2f, y - TABLE_ROW_HEIGHT + 2.2f, widths[index] - 2.4f,
                            TABLE_ROW_HEIGHT - 4.4f, statusColor.red(), statusColor.green(), statusColor.blue());
                    drawCellText(content, cell.value(), x, y, widths[index], TABLE_ROW_HEIGHT, "F2", 5.6f,
                            statusColor.textRed(), statusColor.textGreen(), statusColor.textBlue(), true);
                } else {
                    drawCellText(content, cell.value(), x, y, widths[index], TABLE_ROW_HEIGHT, "F1", 5.6f,
                            0.48f, 0.54f, 0.60f, true);
                }
            } else {
                drawCellText(content, cell.value(), x, y, widths[index], TABLE_ROW_HEIGHT,
                        index < 2 ? "F2" : "F1", 5.5f, 0.13f, 0.20f, 0.28f, index >= 4);
            }
            x += widths[index];
        }
    }

    private List<CellValue> buildBodyCells(AgencyAttendanceReportRow row, int daysInMonth) {
        List<CellValue> cells = new ArrayList<>(4 + daysInMonth + TOTAL_COLUMN_COUNT);
        cells.add(new CellValue(compactEmployeeType(row.employeeType()), false));
        cells.add(new CellValue(safeOrDash(row.requestId()), false));
        cells.add(new CellValue(employeeLabel(row), false));
        cells.add(new CellValue(projectLabel(row), false));

        Map<Integer, String> dailyStatus = row.dailyStatus();
        for (int day = 1; day <= daysInMonth; day++) {
            String status = dailyStatus == null ? "" : dailyStatus.get(day);
            cells.add(new CellValue(normalizeStatus(status), true));
        }

        cells.add(new CellValue(Long.toString(row.presentCount()), false));
        cells.add(new CellValue(Long.toString(row.absentTotalCount()), false));
        cells.add(new CellValue(Long.toString(row.leaveCount()), false));
        cells.add(new CellValue(Long.toString(row.compOffCount()), false));
        cells.add(new CellValue(Long.toString(row.tourCount()), false));
        cells.add(new CellValue(Long.toString(row.holidayCount()), false));
        cells.add(new CellValue(Long.toString(row.weekOffCount()), false));
        cells.add(new CellValue(Long.toString(row.payableDays()), false));
        return cells;
    }

    private void appendFooter(StringBuilder content, LocalDateTime generatedAt, int pageNumber, int totalPages) {
        drawLine(content, LEFT_MARGIN, FOOTER_Y + 10f, PAGE_WIDTH - RIGHT_MARGIN, FOOTER_Y + 10f,
                0.74f, 0.80f, 0.86f, 1f);
        drawText(content, "System generated agency attendance report.", LEFT_MARGIN, FOOTER_Y, "F1", 7.5f,
                0.39f, 0.45f, 0.52f);
        drawText(content, "Generated: " + GENERATED_FORMATTER.format(generatedAt), PAGE_WIDTH - RIGHT_MARGIN - 156f,
                FOOTER_Y, "F1", 7.5f, 0.39f, 0.45f, 0.52f);
        drawText(content, pageNumber + "/" + totalPages, PAGE_WIDTH - RIGHT_MARGIN - 20f, FOOTER_Y, "F1", 7.5f,
                0.39f, 0.45f, 0.52f);
    }

    private void drawInfoCard(
            StringBuilder content,
            float x,
            float y,
            float width,
            float height,
            String label,
            String value) {
        fillRect(content, x, y, width, height, 0.985f, 0.99f, 1f);
        strokeRect(content, x, y, width, height, 0.80f, 0.86f, 0.92f, 1f);
        drawText(content, label, x + 8f, y + height - 11f, "F2", 7f, 0.40f, 0.47f, 0.55f);
        drawText(content, truncate(value, 54), x + 8f, y + 9f, "F1", 8.2f, 0.09f, 0.20f, 0.30f);
    }

    private void drawSummaryCard(
            StringBuilder content,
            float x,
            float y,
            float width,
            float height,
            String label,
            String value) {
        fillRect(content, x, y, width, height, 0.95f, 0.98f, 0.97f);
        strokeRect(content, x, y, width, height, 0.72f, 0.84f, 0.80f, 1f);
        drawText(content, label, x + 8f, y + height - 11f, "F2", 7f, 0.27f, 0.39f, 0.35f);
        drawText(content, truncate(value, 20), x + 8f, y + 10f, "F2", 10f, 0.04f, 0.32f, 0.20f);
    }

    private void drawCellText(
            StringBuilder content,
            String value,
            float x,
            float y,
            float width,
            float height,
            String fontKey,
            float fontSize,
            float red,
            float green,
            float blue,
            boolean centered) {
        String text = truncateForWidth(value, width, fontSize);
        float drawX = centered ? x + Math.max(1.2f, (width - estimateTextWidth(text, fontSize)) / 2f) : x + 2.2f;
        float drawY = y - (height / 2f) - (fontSize / 2.8f);
        drawText(content, text, drawX, drawY, fontKey, fontSize, red, green, blue);
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
                outputStream.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(index))
                        .getBytes(StandardCharsets.US_ASCII));
            }

            outputStream.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n")
                    .getBytes(StandardCharsets.US_ASCII));
            outputStream.write(("startxref\n" + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate agency attendance report PDF.", exception);
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
            throw new IllegalStateException("Unable to generate agency attendance report PDF stream.", exception);
        }
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

    private float[] buildColumnWidths(int reportDaysInMonth) {
        int daysInMonth = normalizeDaysInMonth(reportDaysInMonth);
        float[] widths = new float[4 + daysInMonth + TOTAL_COLUMN_COUNT];
        widths[0] = TYPE_COLUMN_WIDTH;
        widths[1] = REQUEST_COLUMN_WIDTH;
        widths[2] = EMPLOYEE_COLUMN_WIDTH;
        widths[3] = PROJECT_COLUMN_WIDTH;

        float totalColumnsWidth = TOTAL_COLUMN_COUNT * TOTAL_COLUMN_WIDTH;
        float fixedWidth = TYPE_COLUMN_WIDTH + REQUEST_COLUMN_WIDTH + EMPLOYEE_COLUMN_WIDTH + PROJECT_COLUMN_WIDTH
                + totalColumnsWidth;
        float dayWidth = Math.max(14f, Math.min(18f, (CONTENT_WIDTH - fixedWidth) / daysInMonth));
        for (int index = 4; index < 4 + daysInMonth; index++) {
            widths[index] = dayWidth;
        }
        for (int index = 4 + daysInMonth; index < widths.length; index++) {
            widths[index] = TOTAL_COLUMN_WIDTH;
        }
        return widths;
    }

    private int rowCapacity(float tableTop) {
        return Math.max(1, (int) ((tableTop - BOTTOM_MARGIN - TABLE_HEADER_HEIGHT) / TABLE_ROW_HEIGHT));
    }

    private int normalizeDaysInMonth(int daysInMonth) {
        if (daysInMonth < 1 || daysInMonth > 31) {
            return 31;
        }
        return daysInMonth;
    }

    private String employeeLabel(AgencyAttendanceReportRow row) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, row.employeeName());
        addIfPresent(parts, row.employeeCode());
        addIfPresent(parts, row.designation());
        addIfPresent(parts, row.level());
        return parts.isEmpty() ? "-" : String.join(" | ", parts);
    }

    private String projectLabel(AgencyAttendanceReportRow row) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, row.department());
        addIfPresent(parts, row.projectName());
        return parts.isEmpty() ? "-" : String.join(" / ", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private String periodLabel(AgencyAttendanceReportPageView report) {
        AgencyAttendanceReportFilter filter = report.filter();
        if (filter == null) {
            return "-";
        }
        String month = report.monthNames() == null ? null : report.monthNames().get(filter.month());
        return safeOrDash(month) + " " + filter.year();
    }

    private String buildFileName(PdfReportContext context) {
        AgencyAttendanceReportFilter filter = context.report().filter();
        String month = "month";
        String year = Integer.toString(LocalDateTime.now().getYear());
        if (filter != null) {
            month = safeOrDash(context.report().monthNames() == null ? null : context.report().monthNames().get(filter.month()));
            year = Integer.toString(filter.year());
        }
        return "agency-attendance-report-" + fileToken(month) + "-" + year + ".pdf";
    }

    private String formatEmployeeType(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return "All Employees";
        }
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1) + " Employees";
    }

    private String compactEmployeeType(String value) {
        if ("INTERNAL".equalsIgnoreCase(value)) {
            return "Int";
        }
        if ("EXTERNAL".equalsIgnoreCase(value)) {
            return "Ext";
        }
        return safeOrDash(value);
    }

    private String searchLabel(String value) {
        return StringUtils.hasText(value) ? value.trim() : "All employees";
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.length() > 2 ? normalized.substring(0, 2) : normalized;
    }

    private StatusColor statusColor(String status) {
        return switch (safe(status).toUpperCase(Locale.ROOT)) {
            case "P" -> new StatusColor(0.13f, 0.51f, 0.33f, 1f, 1f, 1f);
            case "A" -> new StatusColor(0.72f, 0.25f, 0.27f, 1f, 1f, 1f);
            case "L" -> new StatusColor(0.40f, 0.48f, 0.18f, 1f, 1f, 1f);
            case "CO" -> new StatusColor(0.06f, 0.46f, 0.43f, 1f, 1f, 1f);
            case "T" -> new StatusColor(0.46f, 0.33f, 0.74f, 1f, 1f, 1f);
            case "H" -> new StatusColor(0.15f, 0.44f, 0.75f, 1f, 1f, 1f);
            case "W" -> new StatusColor(0.89f, 0.91f, 0.93f, 0.24f, 0.30f, 0.37f);
            default -> null;
        };
    }

    private AgencyAttendanceReportSummary safeSummary(AgencyAttendanceReportSummary summary) {
        return summary == null
                ? new AgencyAttendanceReportSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                : summary;
    }

    private List<AgencyAttendanceReportRow> safeRows(AgencyAttendanceReportPageView report) {
        return report.rows() == null ? List.of() : report.rows();
    }

    private float sum(float[] values) {
        float total = 0f;
        for (float value : values) {
            total += value;
        }
        return total;
    }

    private String truncateForWidth(String value, float width, float fontSize) {
        int maxLength = Math.max(2, (int) (width / Math.max(1.8f, fontSize * 0.46f)));
        return truncate(value, maxLength);
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safeOrDash(value);
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        if (maxLength <= 3) {
            return safeValue.substring(0, maxLength);
        }
        return safeValue.substring(0, maxLength - 3) + "...";
    }

    private float estimateTextWidth(String value, float fontSize) {
        return sanitizePdfText(value).length() * fontSize * 0.38f;
    }

    private String escapePdfText(String value) {
        return sanitizePdfText(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String sanitizePdfText(String value) {
        String text = safe(value);
        StringBuilder sanitized = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            sanitized.append(character >= 32 && character <= 126 ? character : '?');
        }
        return sanitized.toString();
    }

    private String fileToken(String value) {
        String token = sanitizePdfText(safeOrDash(value))
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return token.isBlank() ? "report" : token;
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String safeOrDash(Object value) {
        String text = safe(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private String formatNumber(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private record PdfReportContext(
            AgencyAttendanceReportPageView report,
            LocalDateTime generatedAt,
            int daysInMonth,
            float[] columnWidths) {
    }

    private record PageLayout(boolean firstPage, List<AgencyAttendanceReportRow> rows) {
    }

    private record CellValue(String value, boolean statusCell) {
    }

    private record StatusColor(
            float red,
            float green,
            float blue,
            float textRed,
            float textGreen,
            float textBlue) {
    }
}
