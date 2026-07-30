package com.maharecruitment.gov.in.invoice.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillLineItemView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillView;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.service.model.GeneratedAgencyMonthlyBillDocument;

@Service
public class AgencyMonthlyBillPdfGenerator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String AUTHORITY_NAME = "Maharashtra Information Technology Corporation Ltd.";
    private static final String DOCUMENT_TITLE = "Agency Monthly Bill Statement";
    private static final String FORM_CODE = "FORM AMB-01";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat(
            "#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private static final float PAGE_WIDTH = 842F;
    private static final float PAGE_HEIGHT = 595F;
    private static final float LEFT_MARGIN = 28F;
    private static final float RIGHT_MARGIN = 28F;
    private static final float TOP_MARGIN = 26F;
    private static final float BOTTOM_MARGIN = 34F;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;
    private static final float ROW_HEIGHT = 22F;
    private static final float SMALL_ROW_HEIGHT = 18F;
    private static final float INFO_ROW_HEIGHT = 34F;
    private static final float CELL_PADDING = 4F;
    private static final float QR_SIZE = 84F;
    private static final float[] EMPLOYEE_TABLE_WIDTHS = {
            28F, 135F, 42F, 115F, 68F, 48F, 88F, 44F, 76F, 62F, 80F
    };

    public GeneratedAgencyMonthlyBillDocument generate(
            AgencyMonthlyBillView bill,
            String preparedByName,
            boolean includeSignatureApproval) {
        if (bill == null) {
            throw new TaxInvoiceException("Agency monthly bill is required for PDF generation.");
        }

        try {
            byte[] bytes = writePdf(bill, preparedByName, includeSignatureApproval);
            return new GeneratedAgencyMonthlyBillDocument(
                    buildFileName(bill),
                    PDF_CONTENT_TYPE,
                    bytes,
                    bytes.length);
        } catch (IOException ex) {
            throw new TaxInvoiceException("Unable to generate agency monthly bill PDF.", ex);
        }
    }

    private byte[] writePdf(
            AgencyMonthlyBillView bill,
            String preparedByName,
            boolean includeSignatureApproval) throws IOException {
        List<PdfPage> pages = paginate(bill, preparedByName, includeSignatureApproval);
        List<PdfObject> objects = new ArrayList<>();

        StringBuilder kids = new StringBuilder();
        int nextObjectId = 5;
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

    private List<PdfPage> paginate(
            AgencyMonthlyBillView bill,
            String preparedByName,
            boolean includeSignatureApproval) {
        List<PdfPage> pages = new ArrayList<>();
        PdfPage page = new PdfPage();
        float y = PAGE_HEIGHT - TOP_MARGIN;

        y = appendDocumentHeader(page, bill, preparedByName, y, true);
        y = appendAmountSummary(page, bill, y);
        y = appendLegend(page, y);
        y = appendSectionCaption(page, "4. Annexure I - Employee-Wise Bill Calculation", y);
        appendEmployeeTableHeader(page, y);
        y -= ROW_HEIGHT;

        List<AgencyMonthlyBillLineItemView> lineItems = bill.getLineItems() == null ? List.of() : bill.getLineItems();
        if (lineItems.isEmpty()) {
            drawText(page, "No billable employees found.", LEFT_MARGIN, y - 12F, 9, PdfFont.REGULAR);
            y -= ROW_HEIGHT;
        } else {
            for (AgencyMonthlyBillLineItemView lineItem : lineItems) {
                if (y - ROW_HEIGHT < BOTTOM_MARGIN) {
                    pages.add(page);
                    page = new PdfPage();
                    y = PAGE_HEIGHT - TOP_MARGIN;
                    y = appendDocumentHeader(page, bill, preparedByName, y, false);
                    y = appendSectionCaption(page, "4. Annexure I - Employee-Wise Bill Calculation (continued)", y);
                    appendEmployeeTableHeader(page, y);
                    y -= ROW_HEIGHT;
                }
                appendEmployeeTableRow(page, lineItem, y);
                y -= ROW_HEIGHT;
            }
        }

        if (y - SMALL_ROW_HEIGHT < BOTTOM_MARGIN) {
            pages.add(page);
            page = new PdfPage();
            y = PAGE_HEIGHT - TOP_MARGIN;
            y = appendDocumentHeader(page, bill, preparedByName, y, false);
        }
        appendEmployeeGrandTotalRow(page, bill, y);
        y -= SMALL_ROW_HEIGHT + 8F;

        y = ensureSpace(pages, page, bill, preparedByName, y, 120F);
        page = pages.isEmpty() ? page : pages.get(pages.size() - 1);
        y = page.currentY == null ? y : page.currentY;

        y = appendCertificate(page, y);
        if (includeSignatureApproval) {
            y = ensureSpace(pages, page, bill, preparedByName, y, 82F);
            page = pages.isEmpty() ? page : pages.get(pages.size() - 1);
            y = page.currentY == null ? y : page.currentY;
            y = appendSignatureApproval(page, y);
        }

        y = ensureSpace(pages, page, bill, preparedByName, y, 116F);
        page = pages.isEmpty() ? page : pages.get(pages.size() - 1);
        y = page.currentY == null ? y : page.currentY;
        appendQrVerification(page, bill, preparedByName, y, includeSignatureApproval ? "7. QR Verification" : "6. QR Verification");

        if (!pages.contains(page)) {
            pages.add(page);
        }

        appendFooters(pages, bill);
        return pages;
    }

    private float ensureSpace(
            List<PdfPage> pages,
            PdfPage page,
            AgencyMonthlyBillView bill,
            String preparedByName,
            float y,
            float requiredHeight) {
        if (y - requiredHeight >= BOTTOM_MARGIN) {
            page.currentY = y;
            return y;
        }

        if (!pages.contains(page)) {
            pages.add(page);
        }
        PdfPage nextPage = new PdfPage();
        float nextY = appendDocumentHeader(nextPage, bill, preparedByName, PAGE_HEIGHT - TOP_MARGIN, false);
        nextPage.currentY = nextY;
        pages.add(nextPage);
        return nextY;
    }

    private float appendDocumentHeader(
            PdfPage page,
            AgencyMonthlyBillView bill,
            String preparedByName,
            float y,
            boolean includeDetails) {
        appendPageFrame(page);
        y = appendOfficialMasthead(page, bill, y, includeDetails);

        if (!includeDetails) {
            return y - 4F;
        }

        y = appendSectionCaption(page, "1. Bill Particulars", y);
        return appendBillParticularsGrid(page, bill, preparedByName, y) - 6F;
    }

    private void appendPageFrame(PdfPage page) {
        float frameX = LEFT_MARGIN - 8F;
        float frameY = BOTTOM_MARGIN - 12F;
        float frameWidth = CONTENT_WIDTH + 16F;
        float frameHeight = PAGE_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN + 18F;
        drawRect(page, frameX, frameY, frameWidth, frameHeight, 0.8F);
        drawRect(page, frameX + 3F, frameY + 3F, frameWidth - 6F, frameHeight - 6F, 0.25F);
    }

    private float appendOfficialMasthead(PdfPage page, AgencyMonthlyBillView bill, float y, boolean includeDetails) {
        float headerHeight = 56F;
        float markWidth = 92F;
        float metaWidth = 140F;
        float centerWidth = CONTENT_WIDTH - markWidth - metaWidth;
        float metaX = LEFT_MARGIN + markWidth + centerWidth;

        drawFilledRect(page, LEFT_MARGIN, y - headerHeight, CONTENT_WIDTH, headerHeight, "0.97");
        drawRect(page, LEFT_MARGIN, y - headerHeight, CONTENT_WIDTH, headerHeight, 0.8F);
        drawLine(page, LEFT_MARGIN + markWidth, y - headerHeight, LEFT_MARGIN + markWidth, y, 0.5F);
        drawLine(page, metaX, y - headerHeight, metaX, y, 0.5F);

        drawRect(page, LEFT_MARGIN + 20F, y - 45F, 52F, 30F, 0.5F);
        drawText(page, "GOVT.", centeredX("GOVT.", 7, LEFT_MARGIN + 20F, 52F), y - 25F, 7, PdfFont.BOLD);
        drawText(page, "MAHARASHTRA", centeredX("MAHARASHTRA", 6, LEFT_MARGIN + 20F, 52F), y - 35F, 6,
                PdfFont.BOLD);

        float centerX = LEFT_MARGIN + markWidth;
        drawText(page, "Government of Maharashtra", centeredX("Government of Maharashtra", 8, centerX, centerWidth),
                y - 12F, 8, PdfFont.BOLD);
        drawText(page, AUTHORITY_NAME, centeredX(AUTHORITY_NAME, 12, centerX, centerWidth), y - 27F, 12,
                PdfFont.BOLD);
        String title = includeDetails ? DOCUMENT_TITLE : DOCUMENT_TITLE + " - Continuation";
        drawText(page, title, centeredX(title, 9, centerX, centerWidth), y - 41F, 9, PdfFont.BOLD);
        drawText(page, "Human Resource / Agency Billing", centeredX("Human Resource / Agency Billing", 6, centerX,
                centerWidth), y - 51F, 6, PdfFont.REGULAR);

        drawFilledRect(page, metaX, y - 18F, metaWidth, 18F, "0.90");
        drawText(page, FORM_CODE, centeredX(FORM_CODE, 8, metaX, metaWidth), y - 11F, 8, PdfFont.BOLD);
        drawText(page, "Bill No.", metaX + 7F, y - 30F, 6, PdfFont.BOLD);
        drawText(page,
                truncateForWidth(defaultText(bill.getBillNumber(), "-"), metaWidth - 56F, 7),
                metaX + 48F,
                y - 30F,
                7,
                PdfFont.REGULAR);
        drawText(page, "Date", metaX + 7F, y - 43F, 6, PdfFont.BOLD);
        drawText(page, formatDate(bill.getGeneratedDate()), metaX + 48F, y - 43F, 7, PdfFont.REGULAR);
        drawText(page, includeDetails ? "Original" : "Continuation",
                rightAlignedX(includeDetails ? "Original" : "Continuation", 6, metaX + metaWidth - 7F),
                y - 53F,
                6,
                PdfFont.BOLD);
        return y - headerHeight - 8F;
    }

    private float appendBillParticularsGrid(
            PdfPage page,
            AgencyMonthlyBillView bill,
            String preparedByName,
            float y) {
        float columnWidth = CONTENT_WIDTH / 4F;
        List<List<InfoCell>> rows = List.of(
                List.of(
                        new InfoCell("Agency Name", defaultText(bill.getAgencyName(), "-"), 2),
                        new InfoCell("Bill Number", defaultText(bill.getBillNumber(), "-"), 1),
                        new InfoCell("Generated Date", formatDate(bill.getGeneratedDate()), 1)),
                List.of(
                        new InfoCell("Bill Period",
                                formatDate(bill.getPeriodFrom()) + " to " + formatDate(bill.getPeriodTo()), 1),
                        new InfoCell("Month / Year", formatMonthYear(bill), 1),
                        new InfoCell("Employee Type", employeeTypeLabel(bill.getEmployeeType()), 1),
                        new InfoCell("Employees Covered", integerText(bill.getEmployeeCount()), 1)),
                List.of(
                        new InfoCell("Prepared By", defaultText(preparedByName, "-"), 2),
                        new InfoCell("Authority", AUTHORITY_NAME, 1),
                        new InfoCell("Document Status", "Generated", 1)));

        for (List<InfoCell> row : rows) {
            float x = LEFT_MARGIN;
            for (InfoCell cell : row) {
                float width = columnWidth * cell.columnSpan();
                appendInfoCell(page, cell, x, y, width);
                x += width;
            }
            y -= INFO_ROW_HEIGHT;
        }
        return y;
    }

    private void appendInfoCell(PdfPage page, InfoCell cell, float x, float y, float width) {
        drawFilledRect(page, x, y - 12F, width, 12F, "0.94");
        drawRect(page, x, y - INFO_ROW_HEIGHT, width, INFO_ROW_HEIGHT, 0.5F);
        drawLine(page, x, y - 12F, x + width, y - 12F, 0.35F);
        drawText(page, truncateForWidth(cell.label(), width - 10F, 6), x + 5F, y - 8F, 6, PdfFont.BOLD);
        drawText(page, truncateForWidth(cell.value(), width - 10F, 8), x + 5F, y - 24F, 8, PdfFont.REGULAR);
    }

    private float appendAmountSummary(PdfPage page, AgencyMonthlyBillView bill, float y) {
        y = appendSectionCaption(page, "2. Amount Summary", y);
        float[] widths = { 190F, 170F, 125F, CONTENT_WIDTH - 485F };
        appendTableRow(page, List.of("Particular", "Rate / Value", "Amount (INR)", "Calculation Basis"),
                widths, y, true);
        y -= SMALL_ROW_HEIGHT;
        appendTableRow(page,
                List.of("Attendance Amount", "Monthly rate x payable days", formatMoney(bill.getAttendanceAmount()),
                        "Monthly rate divided by days in month, multiplied by payable days."),
                widths, y, false);
        y -= SMALL_ROW_HEIGHT;
        appendTableRow(page,
                List.of("Agency Margin", formatPercentage(bill.getAgencyMarginRate()),
                        formatMoney(bill.getAgencyMarginAmount()),
                        "Agency margin calculated on attendance amount as per active commission master."),
                widths, y, false);
        y -= SMALL_ROW_HEIGHT;
        appendTableRow(page,
                List.of("Total Bill Amount", "", formatMoney(bill.getTotalAmount()),
                        "Amount payable subject to audit and competent authority approval."),
                widths, y, true);
        return y - SMALL_ROW_HEIGHT - 8F;
    }

    private float appendLegend(PdfPage page, float y) {
        y = appendSectionCaption(page, "3. Attendance Code Legend", y);
        float[] widths = { 180F, 260F, 160F, CONTENT_WIDTH - 600F };
        appendTableRow(page, List.of("Payable Codes", "Meaning", "Non-Payable Codes", "Meaning"), widths, y, true);
        y -= SMALL_ROW_HEIGHT;
        appendTableRow(page,
                List.of("P / CO / T / H / W", "Present / Comp Off / Approved Tour / Holiday / Week Off",
                        "A / L", "Absent / Approved Leave"),
                widths,
                y,
                false);
        return y - SMALL_ROW_HEIGHT - 8F;
    }

    private float appendSectionCaption(PdfPage page, String title, float y) {
        drawFilledRect(page, LEFT_MARGIN, y - 16F, CONTENT_WIDTH, 16F, "0.92");
        drawFilledRect(page, LEFT_MARGIN, y - 16F, 5F, 16F, "0.42");
        drawRect(page, LEFT_MARGIN, y - 16F, CONTENT_WIDTH, 16F, 0.5F);
        drawText(page, title, LEFT_MARGIN + 10F, y - 11F, 8, PdfFont.BOLD);
        return y - 18F;
    }

    private void appendEmployeeTableHeader(PdfPage page, float y) {
        appendTableRow(page,
                List.of("Sr", "Employee Details", "Type", "Designation / Level", "Monthly Rate", "Pay Days",
                        "P/CO/T/H/W", "A/L", "Attendance", "Margin", "Total"),
                EMPLOYEE_TABLE_WIDTHS,
                y,
                true);
    }

    private void appendEmployeeTableRow(PdfPage page, AgencyMonthlyBillLineItemView line, float y) {
        String employee = defaultText(line.getEmployeeName(), "-");
        if (StringUtils.hasText(line.getEmployeeCode())) {
            employee += " / " + line.getEmployeeCode().trim();
        }
        String designation = defaultText(line.getDesignationName(), "-");
        if (StringUtils.hasText(line.getLevelCode())) {
            designation += " / " + line.getLevelCode().trim();
        }
        appendTableRow(page,
                List.of(
                        integerText(line.getLineNumber()),
                        employee,
                        defaultText(line.getEmployeeType(), "-"),
                        designation,
                        formatMoney(line.getMonthlyRate()),
                        longText(line.getPayableDays()) + " / " + integerText(line.getDaysInMonth()),
                        longText(line.getPresentDays()) + "/" + longText(line.getCompOffDays()) + "/"
                                + longText(line.getTourDays()) + "/" + longText(line.getHolidayDays()) + "/"
                                + longText(line.getWeekOffDays()),
                        longText(line.getAbsentDays()) + "/" + longText(line.getLeaveDays()),
                        formatMoney(line.getAttendanceAmount()),
                        formatMoney(line.getAgencyMarginAmount()),
                        formatMoney(line.getLineTotal())),
                EMPLOYEE_TABLE_WIDTHS,
                y,
                false);
    }

    private void appendEmployeeGrandTotalRow(PdfPage page, AgencyMonthlyBillView bill, float y) {
        appendTableRow(page,
                List.of("", "Grand Total", "", "", "", "", "", "",
                        formatMoney(bill.getAttendanceAmount()),
                        formatMoney(bill.getAgencyMarginAmount()),
                        formatMoney(bill.getTotalAmount())),
                EMPLOYEE_TABLE_WIDTHS,
                y,
                true);
    }

    private float appendCertificate(PdfPage page, float y) {
        y = appendSectionCaption(page, "5. Verification Certificate", y);
        String text = "Certified that the above statement has been generated from the system records for attendance, "
                + "approved tour, comp-off, holiday, week-off entries, designation monthly rate, and applicable agency margin. "
                + "The bill is subject to verification of supporting records and approval by the competent authority.";
        drawRect(page, LEFT_MARGIN, y - 36F, CONTENT_WIDTH, 36F, 0.5F);
        List<String> lines = wrap(text, 150);
        float textY = y - 12F;
        for (String line : lines.subList(0, Math.min(lines.size(), 3))) {
            drawText(page, line, LEFT_MARGIN + 6F, textY, 7, PdfFont.REGULAR);
            textY -= 10F;
        }
        return y - 44F;
    }

    private float appendSignatureApproval(PdfPage page, float y) {
        y = appendSectionCaption(page, "6. Signature And Approval", y);
        float boxWidth = CONTENT_WIDTH / 4F;
        List<String> labels = List.of("Prepared By", "Checked By", "Agency Confirmation", "Approved By");
        List<String> values = List.of("Name / Signature / Date", "HR Verification", "Authorized Signatory",
                "Competent Authority");
        for (int index = 0; index < labels.size(); index++) {
            float x = LEFT_MARGIN + (index * boxWidth);
            drawRect(page, x, y - 44F, boxWidth, 44F, 0.5F);
            drawText(page, labels.get(index), x + 5F, y - 12F, 7, PdfFont.BOLD);
            drawLine(page, x + 8F, y - 32F, x + boxWidth - 8F, y - 32F, 0.5F);
            drawText(page, values.get(index), x + 8F, y - 39F, 6, PdfFont.REGULAR);
        }
        return y - 52F;
    }

    private void appendQrVerification(
            PdfPage page,
            AgencyMonthlyBillView bill,
            String preparedByName,
            float y,
            String title) {
        y = appendSectionCaption(page, title, y);
        drawRect(page, LEFT_MARGIN, y - 96F, CONTENT_WIDTH, 96F, 0.5F);
        float qrX = LEFT_MARGIN + 8F;
        float qrTopY = y - 8F;
        drawQrCode(page, bill, preparedByName, qrX, qrTopY, QR_SIZE);

        float detailX = qrX + QR_SIZE + 16F;
        float detailY = y - 14F;
        drawText(page, "Scan to verify bill details", detailX, detailY, 8, PdfFont.BOLD);
        detailY -= 16F;
        List<List<String>> rows = List.of(
                List.of("Bill ID", longText(bill.getAgencyMonthlyBillId()), "Bill Number", defaultText(bill.getBillNumber(), "-")),
                List.of("Generated Date", formatDate(bill.getGeneratedDate()), "Total Amount", formatMoney(bill.getTotalAmount())),
                List.of("Authority", AUTHORITY_NAME, "Prepared By", defaultText(preparedByName, "-")));
        for (List<String> row : rows) {
            drawText(page, row.get(0) + ": " + row.get(1), detailX, detailY, 7, PdfFont.REGULAR);
            drawText(page, row.get(2) + ": " + row.get(3), detailX + 260F, detailY, 7, PdfFont.REGULAR);
            detailY -= 13F;
        }
    }

    private void drawQrCode(
            PdfPage page,
            AgencyMonthlyBillView bill,
            String preparedByName,
            float x,
            float topY,
            float size) {
        try {
            BitMatrix matrix = createQrMatrix(buildQrPayload(bill, preparedByName));
            int matrixWidth = matrix.getWidth();
            float moduleSize = size / matrixWidth;
            drawFilledRect(page, x, topY - size, size, size, "1");
            drawRect(page, x, topY - size, size, size, 0.5F);
            StringBuilder drawing = new StringBuilder("0 g\n");
            for (int row = 0; row < matrix.getHeight(); row++) {
                for (int column = 0; column < matrixWidth; column++) {
                    if (matrix.get(column, row)) {
                        drawing.append(formatPdfNumber(x + (column * moduleSize))).append(' ')
                                .append(formatPdfNumber(topY - ((row + 1) * moduleSize))).append(' ')
                                .append(formatPdfNumber(moduleSize)).append(' ')
                                .append(formatPdfNumber(moduleSize)).append(" re f\n");
                    }
                }
            }
            page.drawings.add(drawing.toString());
        } catch (WriterException ex) {
            drawRect(page, x, topY - size, size, size, 0.5F);
            drawText(page, "QR unavailable", x + 12F, topY - (size / 2F), 7, PdfFont.REGULAR);
        }
    }

    private BitMatrix createQrMatrix(String payload) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        return new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 72, 72, hints);
    }

    private String buildQrPayload(AgencyMonthlyBillView bill, String preparedByName) {
        return String.join("\n",
                "AMB",
                "ID:" + longText(bill.getAgencyMonthlyBillId()),
                "NO:" + normalizeQrText(bill.getBillNumber()),
                "DATE:" + formatIsoDate(bill.getGeneratedDate()),
                "TOTAL:" + formatPlainAmount(bill.getTotalAmount()),
                "AUTH:MAHAIT",
                "BY:" + normalizeQrText(preparedByName));
    }

    private void appendTableRow(PdfPage page, List<String> cells, float[] widths, float y, boolean header) {
        float tableWidth = sum(widths);
        float rowHeight = widths == EMPLOYEE_TABLE_WIDTHS ? ROW_HEIGHT : SMALL_ROW_HEIGHT;
        if (header) {
            drawFilledRect(page, LEFT_MARGIN, y - rowHeight, tableWidth, rowHeight, "0.91");
        }
        drawRect(page, LEFT_MARGIN, y - rowHeight, tableWidth, rowHeight, 0.5F);

        float nextX = LEFT_MARGIN;
        for (int index = 0; index < widths.length - 1; index++) {
            nextX += widths[index];
            drawLine(page, nextX, y - rowHeight, nextX, y, 0.5F);
        }

        float cellX = LEFT_MARGIN;
        for (int index = 0; index < cells.size() && index < widths.length; index++) {
            int fontSize = 6;
            boolean employeeTable = widths == EMPLOYEE_TABLE_WIDTHS;
            boolean serialColumn = employeeTable && index == 0;
            boolean number = isNumericColumn(cells.get(index), index, widths);
            String text = truncateForWidth(cells.get(index), widths[index] - (CELL_PADDING * 2), fontSize);
            float textX = cellX + CELL_PADDING;
            if (serialColumn) {
                textX = centeredX(text, fontSize, cellX, widths[index]);
            } else if (number) {
                textX = rightAlignedX(text, fontSize, cellX + widths[index] - CELL_PADDING);
            }
            drawText(page, text, textX, y - ((rowHeight + fontSize) / 2F), fontSize,
                    header ? PdfFont.BOLD : PdfFont.REGULAR);
            cellX += widths[index];
        }
    }

    private boolean isNumericColumn(String value, int index, float[] widths) {
        if (widths == EMPLOYEE_TABLE_WIDTHS) {
            return index >= 4;
        }
        return StringUtils.hasText(value) && value.trim().matches("-?\\d[0-9,./% ]*");
    }

    private void appendFooters(List<PdfPage> pages, AgencyMonthlyBillView bill) {
        int totalPages = pages.size();
        for (int index = 0; index < totalPages; index++) {
            PdfPage page = pages.get(index);
            float y = 18F;
            drawLine(page, LEFT_MARGIN, y + 12F, PAGE_WIDTH - RIGHT_MARGIN, y + 12F, 0.5F);
            drawText(page,
                    "System generated on " + formatDateTime(bill.getCreatedDate()),
                    LEFT_MARGIN,
                    y,
                    7,
                    PdfFont.REGULAR);
            String pageText = "Page " + (index + 1) + " of " + totalPages;
            drawText(page, pageText, rightAlignedX(pageText, 7, PAGE_WIDTH - RIGHT_MARGIN), y, 7, PdfFont.REGULAR);
        }
    }

    private byte[] buildPageContent(PdfPage page) {
        StringBuilder content = new StringBuilder();
        for (String drawing : page.drawings) {
            content.append(drawing);
        }
        for (PdfText text : page.texts) {
            appendText(content, text.value(), text.x(), text.y(), text.fontSize(), text.font());
        }
        return content.toString().getBytes(StandardCharsets.US_ASCII);
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

    private void drawText(PdfPage page, String text, float x, float y, int fontSize, PdfFont font) {
        page.texts.add(new PdfText(defaultText(text, ""), x, y, fontSize, font));
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

    private void drawFilledRect(PdfPage page, float x, float y, float width, float height, String grayScale) {
        page.drawings.add(grayScale + " g\n"
                + formatPdfNumber(x) + " " + formatPdfNumber(y) + " "
                + formatPdfNumber(width) + " " + formatPdfNumber(height) + " re f\n"
                + "0 g\n");
    }

    private void drawRect(PdfPage page, float x, float y, float width, float height, float lineWidth) {
        page.drawings.add(formatPdfNumber(lineWidth) + " w\n"
                + formatPdfNumber(x) + " " + formatPdfNumber(y) + " "
                + formatPdfNumber(width) + " " + formatPdfNumber(height) + " re S\n");
    }

    private void drawLine(PdfPage page, float startX, float startY, float endX, float endY, float lineWidth) {
        page.drawings.add(formatPdfNumber(lineWidth) + " w\n"
                + formatPdfNumber(startX) + " " + formatPdfNumber(startY) + " m "
                + formatPdfNumber(endX) + " " + formatPdfNumber(endY) + " l S\n");
    }

    private List<String> wrap(String text, int maxLength) {
        String normalized = defaultText(text, "-").replaceAll("\\s+", " ").trim();
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (current.isEmpty()) {
                current.append(word);
            } else if (current.length() + word.length() + 1 <= maxLength) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("-") : lines;
    }

    private String truncateForWidth(String value, float width, int fontSize) {
        int maxLength = Math.max(2, (int) (width / (fontSize * 0.46F)));
        String normalized = defaultText(value, "");
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, Math.max(0, maxLength - 1)) + ".";
    }

    private float centeredX(String text, int fontSize, float x, float width) {
        float estimatedWidth = estimateTextWidth(text, fontSize);
        return x + Math.max(0F, (width - estimatedWidth) / 2F);
    }

    private float rightAlignedX(String text, int fontSize, float rightEdge) {
        float estimatedWidth = estimateTextWidth(text, fontSize);
        return Math.max(LEFT_MARGIN, rightEdge - estimatedWidth);
    }

    private float estimateTextWidth(String text, int fontSize) {
        return sanitizePdfText(text).length() * fontSize * 0.46F;
    }

    private float sum(float[] values) {
        float total = 0F;
        for (float value : values) {
            total += value;
        }
        return total;
    }

    private String buildFileName(AgencyMonthlyBillView bill) {
        String billNumber = defaultText(bill.getBillNumber(), "agency-monthly-bill");
        return sanitizeFileName("agency-monthly-bill-" + billNumber) + ".pdf";
    }

    private String sanitizeFileName(String value) {
        return defaultText(value, "agency-monthly-bill").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMAT.format(date);
    }

    private String formatIsoDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMAT.format(dateTime);
    }

    private String formatMonthYear(AgencyMonthlyBillView bill) {
        if (bill.getBillMonth() == null || bill.getBillYear() == null) {
            return "-";
        }
        String monthName = java.time.Month.of(bill.getBillMonth())
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return monthName + " " + bill.getBillYear();
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return MONEY_FORMAT.format(safeAmount.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatPlainAmount(BigDecimal amount) {
        return amount == null ? "" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercentage(BigDecimal percentage) {
        return percentage == null ? "0.00%" : formatMoney(percentage) + "%";
    }

    private String employeeTypeLabel(String value) {
        if ("INTERNAL".equalsIgnoreCase(defaultText(value, ""))) {
            return "Internal Employees";
        }
        if ("EXTERNAL".equalsIgnoreCase(defaultText(value, ""))) {
            return "External Employees";
        }
        return "All Employees";
    }

    private String integerText(Integer value) {
        return value == null ? "0" : value.toString();
    }

    private String longText(Long value) {
        return value == null ? "0" : value.toString();
    }

    private String normalizeQrText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('|', ' ')
                .replaceAll("\\s+", " ");
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
        private Float currentY;
    }

    private record InfoCell(String label, String value, int columnSpan) {
    }

    private record PdfText(String value, float x, float y, int fontSize, PdfFont font) {
    }

    private record PdfObject(int id, String content) {
    }
}
