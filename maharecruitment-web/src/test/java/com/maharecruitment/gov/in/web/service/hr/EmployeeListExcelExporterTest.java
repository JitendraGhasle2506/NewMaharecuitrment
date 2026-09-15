package com.maharecruitment.gov.in.web.service.hr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;

class EmployeeListExcelExporterTest {

    private final EmployeeListExcelExporter exporter = new EmployeeListExcelExporter();

    @Test
    void exportsDetailedEmployeeColumnsAsAFilterableSpreadsheet() throws Exception {
        EmployeeListView employee = new EmployeeListView(
                10L,
                "EMP-010",
                "Asha Patil",
                "asha.patil@example.test",
                "Project Manager",
                LocalDate.of(2026, 9, 1),
                "INTERNAL",
                "MAHAIT",
                "Application Cell",
                "Ravi Shah",
                "Meera Joshi",
                "ACTIVE");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        exporter.write(List.of(employee), "ACTIVE", outputStream);

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(outputStream.toByteArray()))) {
            var sheet = workbook.getSheet("Employees");
            var header = sheet.getRow(3);
            var row = sheet.getRow(4);

            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("Employee Name");
            assertThat(header.getCell(8).getStringCellValue()).isEqualTo("Cell");
            assertThat(header.getCell(9).getStringCellValue()).isEqualTo("Reporting Manager");
            assertThat(header.getCell(10).getStringCellValue()).isEqualTo("Reporting HOD");
            assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(1);
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("Asha Patil");
            assertThat(row.getCell(8).getStringCellValue()).isEqualTo("Application Cell");
            assertThat(row.getCell(9).getStringCellValue()).isEqualTo("Ravi Shah");
            assertThat(row.getCell(10).getStringCellValue()).isEqualTo("Meera Joshi");
            assertThat(row.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(6).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getCTWorksheet().isSetAutoFilter()).isTrue();
        }
    }
}
