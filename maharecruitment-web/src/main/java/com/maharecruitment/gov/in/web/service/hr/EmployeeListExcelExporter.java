package com.maharecruitment.gov.in.web.service.hr;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;

@Service
public class EmployeeListExcelExporter {

    private static final int HEADER_ROW_INDEX = 3;
    private static final int MAX_DATA_ROWS_PER_SHEET =
            SpreadsheetVersion.EXCEL2007.getMaxRows() - HEADER_ROW_INDEX - 1;
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm z");

    private static final String[] HEADERS = {
            "Sr. No.",
            "Employee Code",
            "Employee Name",
            "Email",
            "Agency",
            "Designation",
            "MAHAIT Joining Date",
            "Type",
            "Cell",
            "Reporting Manager",
            "Reporting HOD",
            "Status"
    };

    private static final int[] COLUMN_WIDTHS = {
            10, 18, 28, 34, 26, 26, 20, 14, 24, 28, 28, 14
    };

    public void write(List<EmployeeListView> employees, String status, OutputStream outputStream)
            throws IOException {
        List<EmployeeListView> rows = employees == null ? List.of() : employees;
        SXSSFWorkbook workbook = new SXSSFWorkbook(200);
        workbook.setCompressTempFiles(true);

        try (workbook) {
            WorkbookStyles styles = createStyles(workbook);
            int sheetCount = Math.max(1,
                    (rows.size() + MAX_DATA_ROWS_PER_SHEET - 1) / MAX_DATA_ROWS_PER_SHEET);

            for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
                int fromIndex = sheetIndex * MAX_DATA_ROWS_PER_SHEET;
                int toIndex = Math.min(rows.size(), fromIndex + MAX_DATA_ROWS_PER_SHEET);
                writeSheet(workbook, styles, rows, status, sheetIndex, sheetCount, fromIndex, toIndex);
            }

            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.dispose();
        }
    }

    private void writeSheet(
            SXSSFWorkbook workbook,
            WorkbookStyles styles,
            List<EmployeeListView> employees,
            String status,
            int sheetIndex,
            int sheetCount,
            int fromIndex,
            int toIndex) {
        String sheetName = sheetCount == 1 ? "Employees" : "Employees " + (sheetIndex + 1);
        Sheet sheet = workbook.createSheet(sheetName);
        configureSheet(sheet);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Employee Details Report");
        titleCell.setCellStyle(styles.title());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

        Row metadataRow = sheet.createRow(1);
        metadataRow.setHeightInPoints(21);
        Cell metadataCell = metadataRow.createCell(0);
        metadataCell.setCellValue("Status: " + display(status)
                + "  |  Total records: " + employees.size()
                + "  |  Generated: " + ZonedDateTime.now(REPORT_ZONE).format(GENERATED_AT_FORMAT));
        metadataCell.setCellStyle(styles.metadata());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));

        Row headerRow = sheet.createRow(HEADER_ROW_INDEX);
        headerRow.setHeightInPoints(25);
        for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
            Cell headerCell = headerRow.createCell(columnIndex);
            headerCell.setCellValue(HEADERS[columnIndex]);
            headerCell.setCellStyle(styles.header());
        }

        int excelRowIndex = HEADER_ROW_INDEX + 1;
        for (int employeeIndex = fromIndex; employeeIndex < toIndex; employeeIndex++) {
            writeEmployeeRow(
                    sheet.createRow(excelRowIndex++),
                    styles,
                    employees.get(employeeIndex),
                    employeeIndex + 1);
        }

        int filterLastRow = Math.max(HEADER_ROW_INDEX, excelRowIndex - 1);
        sheet.setAutoFilter(new CellRangeAddress(
                HEADER_ROW_INDEX, filterLastRow, 0, HEADERS.length - 1));
        workbook.setPrintArea(
                workbook.getSheetIndex(sheet),
                0,
                HEADERS.length - 1,
                0,
                filterLastRow);
    }

    private void configureSheet(Sheet sheet) {
        sheet.createFreezePane(0, HEADER_ROW_INDEX + 1);
        sheet.setAutobreaks(true);
        sheet.setRepeatingRows(new CellRangeAddress(0, HEADER_ROW_INDEX, -1, -1));
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
        sheet.setFitToPage(true);

        for (int columnIndex = 0; columnIndex < COLUMN_WIDTHS.length; columnIndex++) {
            sheet.setColumnWidth(columnIndex, COLUMN_WIDTHS[columnIndex] * 256);
        }
    }

    private void writeEmployeeRow(
            Row row,
            WorkbookStyles styles,
            EmployeeListView employee,
            int serialNumber) {
        Cell serialCell = row.createCell(0);
        serialCell.setCellValue(serialNumber);
        serialCell.setCellStyle(styles.number());

        writeTextCell(row, 1, employee.employeeCode(), styles.text());
        writeTextCell(row, 2, employee.fullName(), styles.text());
        writeTextCell(row, 3, employee.email(), styles.text());
        writeTextCell(row, 4, employee.agencyName(), styles.text());
        writeTextCell(row, 5, employee.designation(), styles.text());

        Cell joiningDateCell = row.createCell(6);
        if (employee.mahaitJoiningDate() == null) {
            joiningDateCell.setCellValue("-");
            joiningDateCell.setCellStyle(styles.text());
        } else {
            joiningDateCell.setCellValue(Date.valueOf(employee.mahaitJoiningDate()));
            joiningDateCell.setCellStyle(styles.date());
        }

        writeTextCell(row, 7, employee.recruitmentType(), styles.centeredText());
        writeTextCell(row, 8, employee.cellName(), styles.text());
        writeTextCell(row, 9, employee.reportingManagerName(), styles.text());
        writeTextCell(row, 10, employee.reportingHodName(), styles.text());
        writeTextCell(row, 11, employee.status(), styles.centeredText());
    }

    private void writeTextCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(display(value));
        cell.setCellStyle(style);
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private WorkbookStyles createStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Font metadataFont = workbook.createFont();
        metadataFont.setItalic(true);
        metadataFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

        CellStyle metadataStyle = workbook.createCellStyle();
        metadataStyle.setFont(metadataFont);
        metadataStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        metadataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        metadataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        applyBorders(headerStyle);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        textStyle.setWrapText(false);
        applyBorders(textStyle);

        CellStyle centeredTextStyle = workbook.createCellStyle();
        centeredTextStyle.cloneStyleFrom(textStyle);
        centeredTextStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(centeredTextStyle);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(centeredTextStyle);
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd-mm-yyyy"));

        return new WorkbookStyles(
                titleStyle,
                metadataStyle,
                headerStyle,
                textStyle,
                centeredTextStyle,
                numberStyle,
                dateStyle);
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        short borderColor = IndexedColors.GREY_25_PERCENT.getIndex();
        style.setTopBorderColor(borderColor);
        style.setRightBorderColor(borderColor);
        style.setBottomBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
    }

    private record WorkbookStyles(
            CellStyle title,
            CellStyle metadata,
            CellStyle header,
            CellStyle text,
            CellStyle centeredText,
            CellStyle number,
            CellStyle date) {
    }
}
