package com.maharecruitment.gov.in.web.service.agency.impl;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.dto.AttendanceReportDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.InternalEmployeeAttendanceReportService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.agency.AgencyAttendanceReportPageService;
import com.maharecruitment.gov.in.web.service.agency.AgencyUserContext;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportFilter;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportPageView;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportRow;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyAttendanceReportSummary;

@Service
@Transactional(readOnly = true)
public class AgencyAttendanceReportPageServiceImpl implements AgencyAttendanceReportPageService {

    private static final int FIRST_REPORT_YEAR = 2020;
    private static final String EMPLOYEE_TYPE_ALL = "ALL";
    private static final String EMPLOYEE_TYPE_EXTERNAL = "EXTERNAL";
    private static final String EMPLOYEE_TYPE_INTERNAL = "INTERNAL";

    private final AttendanceRegisterService attendanceRegisterService;
    private final InternalEmployeeAttendanceReportService internalAttendanceReportService;
    private final AgencyAccessService agencyAccessService;

    public AgencyAttendanceReportPageServiceImpl(
            AttendanceRegisterService attendanceRegisterService,
            InternalEmployeeAttendanceReportService internalAttendanceReportService,
            AgencyAccessService agencyAccessService) {
        this.attendanceRegisterService = attendanceRegisterService;
        this.internalAttendanceReportService = internalAttendanceReportService;
        this.agencyAccessService = agencyAccessService;
    }

    @Override
    public AgencyAttendanceReportPageView getAttendanceReport(
            String actorEmail,
            Integer month,
            Integer year,
            String employeeType,
            String search) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        LocalDate today = LocalDate.now();
        int selectedMonth = resolveMonth(month, today);
        int selectedYear = resolveYear(year, today);
        String normalizedEmployeeType = normalizeEmployeeType(employeeType);
        String normalizedSearch = normalizeSearch(search);

        List<AgencyAttendanceReportRow> rows = Stream
                .concat(
                        loadExternalRows(context.agencyId(), selectedMonth, selectedYear, normalizedEmployeeType,
                                normalizedSearch).stream(),
                        loadInternalRows(context.agencyId(), selectedMonth, selectedYear, normalizedEmployeeType,
                                normalizedSearch).stream())
                .sorted(Comparator
                        .comparing(AgencyAttendanceReportRow::employeeType, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AgencyAttendanceReportRow::employeeName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(row -> safeString(row.requestId()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(row -> row.employeeId() == null ? Long.MAX_VALUE : row.employeeId()))
                .toList();

        AgencyAttendanceReportFilter filter = new AgencyAttendanceReportFilter(
                selectedMonth,
                selectedYear,
                normalizedEmployeeType,
                normalizedSearch);

        return new AgencyAttendanceReportPageView(
                context.agencyId(),
                context.agencyName(),
                filter,
                buildMonthNames(),
                buildYearOptions(selectedYear, today.getYear()),
                YearMonth.of(selectedYear, selectedMonth).lengthOfMonth(),
                List.of(EMPLOYEE_TYPE_ALL, EMPLOYEE_TYPE_EXTERNAL, EMPLOYEE_TYPE_INTERNAL),
                rows,
                buildSummary(rows));
    }

    private List<AgencyAttendanceReportRow> loadExternalRows(
            Long agencyId,
            int month,
            int year,
            String employeeType,
            String search) {
        if (EMPLOYEE_TYPE_INTERNAL.equals(employeeType)) {
            return List.of();
        }

        return attendanceRegisterService
                .getExternalAttendanceReportData(null, agencyId, month, year, null)
                .stream()
                .filter(row -> matchesSearch(row, search))
                .map(this::toExternalRow)
                .toList();
    }

    private List<AgencyAttendanceReportRow> loadInternalRows(
            Long agencyId,
            int month,
            int year,
            String employeeType,
            String search) {
        if (EMPLOYEE_TYPE_EXTERNAL.equals(employeeType)) {
            return List.of();
        }

        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setAgencyId(agencyId);
        filter.setMonth(month);
        filter.setYear(year);
        filter.setEmployeeStatus("ALL");
        filter.setSearchText(search);

        List<InternalAttendanceReportRow> internalRows = internalAttendanceReportService.buildReport(filter).getRows();
        if (internalRows == null || internalRows.isEmpty()) {
            return List.of();
        }

        return internalRows.stream()
                .map(this::toInternalRow)
                .toList();
    }

    private AgencyUserContext resolveAgencyUserContext(String actorEmail) {
        return agencyAccessService.requireActiveAgencyContext(actorEmail);
    }

    private int resolveMonth(Integer month, LocalDate today) {
        if (month == null || month < 1 || month > 12) {
            return today.getMonthValue();
        }
        return month;
    }

    private int resolveYear(Integer year, LocalDate today) {
        int maxYear = today.getYear() + 1;
        if (year == null || year < FIRST_REPORT_YEAR || year > maxYear) {
            return today.getYear();
        }
        return year;
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : "";
    }

    private String normalizeEmployeeType(String employeeType) {
        if (!StringUtils.hasText(employeeType)) {
            return EMPLOYEE_TYPE_ALL;
        }

        String normalized = employeeType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case EMPLOYEE_TYPE_INTERNAL, EMPLOYEE_TYPE_EXTERNAL -> normalized;
            default -> EMPLOYEE_TYPE_ALL;
        };
    }

    private boolean matchesSearch(AttendanceReportDTO row, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }

        String term = search.toLowerCase(Locale.ROOT);
        return contains(row.getEmployeeName(), term)
                || contains(row.getRequestId(), term)
                || contains(row.getDesignation(), term)
                || contains(row.getDepartment(), term)
                || contains(row.getProjectName(), term)
                || contains(row.getLevel(), term);
    }

    private AgencyAttendanceReportRow toExternalRow(AttendanceReportDTO row) {
        return new AgencyAttendanceReportRow(
                EMPLOYEE_TYPE_EXTERNAL,
                row.getUserId(),
                null,
                row.getRequestId(),
                safeString(row.getEmployeeName()),
                safeString(row.getDesignation()),
                safeString(row.getDepartment()),
                safeString(row.getSubDepartment()),
                safeString(row.getProjectName()),
                safeString(row.getLevel()),
                safeString(row.getAgencyName()),
                row.getDailyStatus(),
                row.getPresentCount(),
                row.getAbsentCount(),
                row.getLeaveCount(),
                row.getCompOffCount(),
                row.getTourCount(),
                row.getHolidayCount(),
                row.getWeekOffCount(),
                row.getPayableDays());
    }

    private AgencyAttendanceReportRow toInternalRow(InternalAttendanceReportRow row) {
        return new AgencyAttendanceReportRow(
                EMPLOYEE_TYPE_INTERNAL,
                row.getEmployeeId(),
                row.getEmployeeCode(),
                row.getRequestId(),
                safeString(row.getEmployeeName()),
                safeString(row.getDesignation()),
                safeString(row.getDepartmentName()),
                safeString(row.getSubDepartmentName()),
                safeString(row.getProjectName()),
                safeString(row.getLevelCode()),
                safeString(row.getAgencyName()),
                row.getDailyStatus(),
                row.getPresentCount(),
                row.getAbsentCount(),
                row.getLeaveCount(),
                row.getCompOffCount(),
                row.getTourCount(),
                row.getHolidayCount(),
                row.getWeekOffCount(),
                row.getPayableDays());
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private AgencyAttendanceReportSummary buildSummary(List<AgencyAttendanceReportRow> rows) {
        return new AgencyAttendanceReportSummary(
                rows.size(),
                (int) rows.stream().filter(row -> EMPLOYEE_TYPE_INTERNAL.equals(row.employeeType())).count(),
                (int) rows.stream().filter(row -> EMPLOYEE_TYPE_EXTERNAL.equals(row.employeeType())).count(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::presentCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::absentCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::leaveCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::compOffCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::tourCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::holidayCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::weekOffCount).sum(),
                rows.stream().mapToLong(AgencyAttendanceReportRow::payableDays).sum());
    }

    private Map<Integer, String> buildMonthNames() {
        Map<Integer, String> monthNames = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthNames.put(i + 1, months[i]);
        }
        return monthNames;
    }

    private List<Integer> buildYearOptions(int selectedYear, int currentYear) {
        int maxYear = Math.max(selectedYear, currentYear + 1);
        return IntStream.rangeClosed(FIRST_REPORT_YEAR, maxYear)
                .boxed()
                .toList();
    }

    private String safeString(String value) {
        return Objects.toString(value, "");
    }

}
