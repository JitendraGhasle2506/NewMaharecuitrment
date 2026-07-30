package com.maharecruitment.gov.in.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.service.model.GeneratedAttendanceReportDocument;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;

@ExtendWith(MockitoExtension.class)
class InternalAttendanceReportTimeCsvGeneratorTest {

    @Mock
    private DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    private InternalAttendanceReportTimeCsvGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new InternalAttendanceReportTimeCsvGenerator(dailyAttendanceInternalRepository);
    }

    @Test
    void generateBuildsCsvWithFullMonthInAndOutTimeData() {
        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setMonth(5);
        filter.setYear(2026);

        InternalAttendanceReportRow row = new InternalAttendanceReportRow();
        row.setEmployeeId(101L);
        row.setEmployeeCode("EMP000101");
        row.setEmployeeName("Aarav Sharma");
        row.setAgencyName("Talent Hive");
        LinkedHashMap<Integer, String> dailyStatus = new LinkedHashMap<>();
        dailyStatus.put(1, "P");
        dailyStatus.put(2, "A");
        dailyStatus.put(3, "");
        row.setDailyStatus(dailyStatus);

        InternalAttendanceReportView report = new InternalAttendanceReportView();
        report.setFilter(filter);
        report.setMonthName("May");
        report.setStartDate(LocalDate.of(2026, 5, 1));
        report.setEndDate(LocalDate.of(2026, 5, 31));
        report.setCalendarDays(List.of(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3)));
        report.setRows(List.of(row));

        DailyAttendanceInternalEntity attendance = new DailyAttendanceInternalEntity();
        attendance.setId(1L);
        attendance.setEmployeeId(101L);
        attendance.setAttendanceDate(LocalDate.of(2026, 5, 1));
        attendance.setInTime("09:02");
        attendance.setOutTime("18:10");
        attendance.setTotalHours("09:08");
        attendance.setStatus("PRESENT");

        when(dailyAttendanceInternalRepository.findByEmployeeIdInAndAttendanceDateBetween(
                List.of(101L),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(attendance));

        GeneratedAttendanceReportDocument document = generator.generate(report);

        assertEquals("maha-recruitment-internal-attendance-in-out-time-report-may-2026.csv", document.originalFileName());
        assertEquals("text/csv", document.contentType());
        assertTrue(document.size() > 0);

        String csv = new String(document.bytes(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"Employee Code\",\"Employee Name\",\"Agency Name\",\"Payable Days Till Date\",\"Date\",\"Day\",\"Status\",\"In Time\",\"Out Time\",\"Total Hours\""));
        assertTrue(csv.contains("\"EMP000101\",\"Aarav Sharma\",\"Talent Hive\",\"0\",\"01-05-2026\",\"Fri\",\"P\",\"09:02\",\"18:10\",\"09:08\""));
        assertTrue(csv.contains("\"EMP000101\",\"Aarav Sharma\",\"Talent Hive\",\"0\",\"02-05-2026\",\"Sat\",\"A\",\"\",\"\",\"\""));
        assertFalse(csv.contains("\"03-05-2026\""));
    }
}
