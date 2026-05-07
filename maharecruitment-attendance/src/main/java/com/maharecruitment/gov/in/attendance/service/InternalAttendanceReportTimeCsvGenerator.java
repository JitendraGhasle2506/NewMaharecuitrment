package com.maharecruitment.gov.in.attendance.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.service.model.GeneratedAttendanceReportDocument;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;

@Service
public class InternalAttendanceReportTimeCsvGenerator {

    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);

    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;

    public InternalAttendanceReportTimeCsvGenerator(
            DailyAttendanceInternalRepository dailyAttendanceInternalRepository) {
        this.dailyAttendanceInternalRepository = dailyAttendanceInternalRepository;
    }

    public GeneratedAttendanceReportDocument generate(InternalAttendanceReportView report) {
        byte[] bytes = buildCsv(report).getBytes(StandardCharsets.UTF_8);
        return new GeneratedAttendanceReportDocument(
                buildFileName(report),
                CSV_CONTENT_TYPE,
                bytes,
                bytes.length);
    }

    private String buildCsv(InternalAttendanceReportView report) {
        StringBuilder csv = new StringBuilder();
        appendRow(
                csv,
                "Employee Code",
                "Employee Name",
                "Agency Name",
                "Date",
                "Day",
                "Status",
                "In Time",
                "Out Time",
                "Total Hours");

        List<InternalAttendanceReportRow> rows =
                report != null && report.getRows() != null ? report.getRows() : List.of();
        List<LocalDate> calendarDays = resolveCalendarDays(report);
        Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> attendanceByEmployee = loadAttendanceByEmployee(report, rows);

        for (InternalAttendanceReportRow row : rows) {
            Map<LocalDate, DailyAttendanceInternalEntity> attendanceByDate =
                    attendanceByEmployee.getOrDefault(row.getEmployeeId(), Map.of());
            for (LocalDate calendarDay : calendarDays) {
                String status = resolveStatus(row, calendarDay);
                DailyAttendanceInternalEntity attendance = attendanceByDate.get(calendarDay);
                if (!shouldExport(status, attendance)) {
                    continue;
                }

                appendRow(
                        csv,
                        row.getEmployeeCode(),
                        row.getEmployeeName(),
                        row.getAgencyName(),
                        DATE_FORMAT.format(calendarDay),
                        calendarDay.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        status,
                        attendance != null ? attendance.getInTime() : null,
                        attendance != null ? attendance.getOutTime() : null,
                        attendance != null ? attendance.getTotalHours() : null);
            }
        }

        return csv.toString();
    }

    private List<LocalDate> resolveCalendarDays(InternalAttendanceReportView report) {
        if (report != null && report.getCalendarDays() != null) {
            return report.getCalendarDays();
        }
        if (report == null || report.getStartDate() == null || report.getEndDate() == null) {
            return List.of();
        }
        return report.getStartDate().datesUntil(report.getEndDate().plusDays(1)).toList();
    }

    private Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> loadAttendanceByEmployee(
            InternalAttendanceReportView report,
            List<InternalAttendanceReportRow> rows) {
        if (report == null || report.getStartDate() == null || report.getEndDate() == null) {
            return Map.of();
        }

        List<Long> employeeIds = rows.stream()
                .map(InternalAttendanceReportRow::getEmployeeId)
                .filter(employeeId -> employeeId != null && employeeId > 0)
                .distinct()
                .toList();

        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        return dailyAttendanceInternalRepository.findByEmployeeIdInAndAttendanceDateBetween(
                employeeIds,
                report.getStartDate(),
                report.getEndDate())
                .stream()
                .collect(Collectors.groupingBy(
                        DailyAttendanceInternalEntity::getEmployeeId,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                DailyAttendanceInternalEntity::getAttendanceDate,
                                attendance -> attendance,
                                this::pickLatestAttendanceRow,
                                LinkedHashMap::new)));
    }

    private String resolveStatus(InternalAttendanceReportRow row, LocalDate calendarDay) {
        if (row == null || row.getDailyStatus() == null || calendarDay == null) {
            return "";
        }
        return normalizeText(row.getDailyStatus().get(calendarDay.getDayOfMonth()));
    }

    private boolean shouldExport(String status, DailyAttendanceInternalEntity attendance) {
        return StringUtils.hasText(status)
                || hasText(attendance != null ? attendance.getInTime() : null)
                || hasText(attendance != null ? attendance.getOutTime() : null)
                || hasText(attendance != null ? attendance.getTotalHours() : null);
    }

    private DailyAttendanceInternalEntity pickLatestAttendanceRow(
            DailyAttendanceInternalEntity left,
            DailyAttendanceInternalEntity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId == null) {
            return right;
        }
        if (rightId == null) {
            return left;
        }
        return rightId >= leftId ? right : left;
    }

    private String buildFileName(InternalAttendanceReportView report) {
        String monthName = report != null && StringUtils.hasText(report.getMonthName())
                ? report.getMonthName().trim().toLowerCase(Locale.ENGLISH)
                : "report";
        String year = report != null && report.getFilter() != null && report.getFilter().getYear() != null
                ? report.getFilter().getYear().toString()
                : "data";
        return "internal-attendance-in-out-report-" + monthName + "-" + year + ".csv";
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append('"')
                    .append(escapeCsv(normalizeText(values[index])))
                    .append('"');
        }
        csv.append("\r\n");
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : "";
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private String escapeCsv(String value) {
        return value.replace("\"", "\"\"");
    }
}
