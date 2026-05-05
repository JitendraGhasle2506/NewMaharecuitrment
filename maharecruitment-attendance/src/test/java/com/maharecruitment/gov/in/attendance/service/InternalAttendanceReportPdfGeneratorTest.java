package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.attendance.service.model.GeneratedAttendanceReportDocument;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportSummary;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;

class InternalAttendanceReportPdfGeneratorTest {

    private final InternalAttendanceReportPdfGenerator generator = new InternalAttendanceReportPdfGenerator();

    @Test
    void generateCreatesPdfDocumentForAttendanceReport() {
        InternalAttendanceReportView report = new InternalAttendanceReportView();
        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setMonth(5);
        filter.setYear(2026);
        filter.setSearchText("Akanksha");
        report.setFilter(filter);
        report.setMonthName("May");
        report.setGeneratedAt(LocalDateTime.of(2026, 5, 5, 17, 20));
        report.setStartDate(LocalDate.of(2026, 5, 1));
        report.setDaysInMonth(31);
        report.setCalendarDays(LocalDate.of(2026, 5, 1).datesUntil(LocalDate.of(2026, 6, 1)).toList());

        InternalAttendanceReportSummary summary = new InternalAttendanceReportSummary();
        summary.setEmployeeCount(1);
        summary.setTotalDaysInMonth(31);
        summary.setOfficeDayCount(5);
        summary.setTotalHolidayCount(1);
        summary.setTotalWeekOffCount(4);
        report.setSummary(summary);

        InternalAttendanceReportRow row = new InternalAttendanceReportRow();
        row.setEmployeeCode("EMP000001");
        row.setEmployeeName("Akanksha Surve");
        row.setAgencyName("Talent Hive");
        row.setDesignation("Java Developer");
        row.setLevelCode("L4");
        LinkedHashMap<Integer, String> dailyStatus = new LinkedHashMap<>();
        dailyStatus.put(1, "P");
        dailyStatus.put(2, "A");
        dailyStatus.put(3, "L");
        row.setDailyStatus(dailyStatus);
        row.setPresentCount(3);
        row.setAbsentCount(1);
        row.setLeaveCount(1);
        row.setTourCount(0);
        row.setHolidayCount(1);
        row.setWeekOffCount(1);
        report.setRows(List.of(row));

        GeneratedAttendanceReportDocument document = generator.generate(report);

        assertEquals("internal-attendance-report-may-2026.pdf", document.originalFileName());
        assertEquals("application/pdf", document.contentType());
        assertTrue(document.size() > 0);
        assertTrue(document.bytes().length > 0);
        assertArrayEquals("%PDF-1.4".getBytes(StandardCharsets.US_ASCII),
                new String(document.bytes(), 0, 8, StandardCharsets.US_ASCII).getBytes(StandardCharsets.US_ASCII));

        String pdfText = new String(document.bytes(), StandardCharsets.US_ASCII);
        assertTrue(pdfText.contains("Generated On: 05-05-2026 05:20 pm"));
        assertEquals(1, pdfText.split("Generated On:", -1).length - 1);
        assertTrue(pdfText.contains("Talent Hive"));
        assertTrue(pdfText.contains("This report is system generated."));
        assertTrue(pdfText.contains("Page 1 of 1"));
    }
}
