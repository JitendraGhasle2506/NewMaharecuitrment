package com.maharecruitment.gov.in.attendance.service.impl;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.service.InternalEmployeeAttendanceReportService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportSummary;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class InternalEmployeeAttendanceReportServiceImpl implements InternalEmployeeAttendanceReportService {

    private static final Logger log = LoggerFactory.getLogger(InternalEmployeeAttendanceReportServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final EmployeeReportingMappingRepository employeeReportingMappingRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final ProjectMstRepository projectRepository;
    private final Clock clock;

    @Autowired
    public InternalEmployeeAttendanceReportServiceImpl(
            EmployeeRepository employeeRepository,
            EmployeeReportingMappingRepository employeeReportingMappingRepository,
            DailyAttendanceInternalRepository dailyAttendanceInternalRepository,
            HolidayRepository holidayRepository,
            LeaveApplicationRepository leaveApplicationRepository,
            TourApplicationRepository tourApplicationRepository,
            ProjectMstRepository projectRepository) {
        this(
                employeeRepository,
                employeeReportingMappingRepository,
                dailyAttendanceInternalRepository,
                holidayRepository,
                leaveApplicationRepository,
                tourApplicationRepository,
                projectRepository,
                Clock.systemDefaultZone());
    }

    InternalEmployeeAttendanceReportServiceImpl(
            EmployeeRepository employeeRepository,
            EmployeeReportingMappingRepository employeeReportingMappingRepository,
            DailyAttendanceInternalRepository dailyAttendanceInternalRepository,
            HolidayRepository holidayRepository,
            LeaveApplicationRepository leaveApplicationRepository,
            TourApplicationRepository tourApplicationRepository,
            ProjectMstRepository projectRepository,
            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.employeeReportingMappingRepository = employeeReportingMappingRepository;
        this.dailyAttendanceInternalRepository = dailyAttendanceInternalRepository;
        this.holidayRepository = holidayRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.tourApplicationRepository = tourApplicationRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Override
    public InternalAttendanceReportView buildReport(InternalAttendanceReportFilter filter) {
        InternalAttendanceReportFilter normalizedFilter = normalizeFilter(filter);
        YearMonth yearMonth = YearMonth.of(normalizedFilter.getYear(), normalizedFilter.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now(clock);
        Set<LocalDate> holidayDates = holidayRepository.findByHolidayDateBetween(startDate, endDate)
                .stream()
                .map(HolidayMasterEntity::getHolidayDate)
                .collect(Collectors.toSet());

        log.debug(
                "Building internal attendance report. month={}, year={}, agencyId={}, departmentRegistrationId={}, subDepartmentId={}, projectId={}, employeeStatus={}, searchText={}",
                normalizedFilter.getMonth(),
                normalizedFilter.getYear(),
                normalizedFilter.getAgencyId(),
                normalizedFilter.getDepartmentRegistrationId(),
                normalizedFilter.getSubDepartmentId(),
                normalizedFilter.getProjectId(),
                normalizedFilter.getEmployeeStatus(),
                normalizedFilter.getSearchText());

        List<EmployeeEntity> employees = employeeRepository.findDetailedInternalEmployeesForAttendanceReport(
                normalizedFilter.getAgencyId(),
                normalizedFilter.getDepartmentRegistrationId(),
                normalizedFilter.getSubDepartmentId(),
                resolveEmployeeStatusFilter(normalizedFilter.getEmployeeStatus()));

        if (employees.isEmpty()) {
            return buildEmptyView(normalizedFilter, startDate, endDate, holidayDates);
        }

        List<Long> employeeIds = employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .filter(id -> id != null && id > 0)
                .toList();

        Map<Long, EmployeeReportingMappingEntity> reportingMappings = employeeReportingMappingRepository
                .findByEmployeeIdIn(employeeIds)
                .stream()
                .filter(mapping -> mapping.getEmployeeId() != null)
                .collect(Collectors.toMap(
                        EmployeeReportingMappingEntity::getEmployeeId,
                        Function.identity(),
                        this::pickPreferredReportingMapping,
                        LinkedHashMap::new));

        Map<Long, ProjectMst> mappedProjects = projectRepository.findAllById(reportingMappings.values().stream()
                .map(EmployeeReportingMappingEntity::getProjectId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProjectMst::getProjectId, Function.identity()));

        List<EmployeeEntity> filteredEmployees = employees.stream()
                .filter(employee -> matchesSearch(employee, reportingMappings.get(employee.getEmployeeId()), mappedProjects,
                        normalizedFilter.getSearchText()))
                .filter(employee -> matchesProject(employee, reportingMappings.get(employee.getEmployeeId()), mappedProjects,
                        normalizedFilter.getProjectId()))
                .sorted(Comparator.comparing(
                        (EmployeeEntity employee) -> normalizeForSort(employee.getFullName()))
                        .thenComparing(EmployeeEntity::getEmployeeId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        if (filteredEmployees.isEmpty()) {
            return buildEmptyView(normalizedFilter, startDate, endDate, holidayDates);
        }

        List<Long> filteredEmployeeIds = filteredEmployees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .filter(id -> id != null && id > 0)
                .toList();

        Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> attendanceByEmployee = dailyAttendanceInternalRepository
                .findByEmployeeIdInAndAttendanceDateBetween(filteredEmployeeIds, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(
                        DailyAttendanceInternalEntity::getEmployeeId,
                        Collectors.toMap(
                                DailyAttendanceInternalEntity::getAttendanceDate,
                                Function.identity(),
                                this::pickLatestAttendanceRow,
                                LinkedHashMap::new)));

        Map<Long, List<LeaveApplicationEntity>> approvedLeavesByEmployee = leaveApplicationRepository
                .findByEmployeeIdInAndStatusOrderByApplicationDateDesc(filteredEmployeeIds, "APPROVED")
                .stream()
                .collect(Collectors.groupingBy(LeaveApplicationEntity::getEmployeeId));

        Map<Long, List<TourApplicationEntity>> approvedToursByEmployee = tourApplicationRepository
                .findByEmployeeIdInAndStatusOrderByApplicationDateDesc(filteredEmployeeIds, "APPROVED")
                .stream()
                .collect(Collectors.groupingBy(TourApplicationEntity::getEmployeeId));

        List<InternalAttendanceReportRow> rows = filteredEmployees.stream()
                .map(employee -> buildRow(
                        employee,
                        reportingMappings.get(employee.getEmployeeId()),
                        mappedProjects,
                        attendanceByEmployee.getOrDefault(employee.getEmployeeId(), Map.of()),
                        approvedLeavesByEmployee.getOrDefault(employee.getEmployeeId(), List.of()),
                        approvedToursByEmployee.getOrDefault(employee.getEmployeeId(), List.of()),
                        holidayDates,
                        startDate,
                        endDate,
                        today))
                .toList();

        InternalAttendanceReportSummary summary = buildSummary(rows, startDate, endDate, holidayDates, today);

        InternalAttendanceReportView view = new InternalAttendanceReportView();
        view.setFilter(normalizedFilter);
        view.setMonthName(yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        view.setStartDate(startDate);
        view.setEndDate(endDate);
        view.setGeneratedAt(LocalDateTime.now(clock));
        view.setDaysInMonth(yearMonth.lengthOfMonth());
        view.setCalendarDays(startDate.datesUntil(endDate.plusDays(1)).toList());
        view.setSummary(summary);
        view.setRows(rows);
        return view;
    }

    private InternalAttendanceReportView buildEmptyView(
            InternalAttendanceReportFilter filter,
            LocalDate startDate,
            LocalDate endDate,
            Set<LocalDate> holidayDates) {
        YearMonth yearMonth = YearMonth.from(startDate);
        InternalAttendanceReportSummary summary = buildSummary(
                List.of(),
                startDate,
                endDate,
                holidayDates,
                LocalDate.now(clock));

        InternalAttendanceReportView view = new InternalAttendanceReportView();
        view.setFilter(filter);
        view.setMonthName(yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        view.setStartDate(startDate);
        view.setEndDate(endDate);
        view.setGeneratedAt(LocalDateTime.now(clock));
        view.setDaysInMonth(yearMonth.lengthOfMonth());
        view.setCalendarDays(startDate.datesUntil(endDate.plusDays(1)).toList());
        view.setSummary(summary);
        view.setRows(List.of());
        return view;
    }

    private InternalAttendanceReportFilter normalizeFilter(InternalAttendanceReportFilter filter) {
        LocalDate today = LocalDate.now(clock);
        InternalAttendanceReportFilter normalizedFilter = new InternalAttendanceReportFilter();
        normalizedFilter.setAgencyId(normalizeEntityId(filter != null ? filter.getAgencyId() : null));
        normalizedFilter.setDepartmentRegistrationId(filter != null ? filter.getDepartmentRegistrationId() : null);
        normalizedFilter.setSubDepartmentId(filter != null ? filter.getSubDepartmentId() : null);
        normalizedFilter.setProjectId(filter != null ? filter.getProjectId() : null);
        normalizedFilter.setEmployeeStatus(normalizeEmployeeStatus(filter != null ? filter.getEmployeeStatus() : null));
        normalizedFilter.setSearchText(normalizeSearchText(filter != null ? filter.getSearchText() : null));
        normalizedFilter.setMonth(resolveMonth(filter != null ? filter.getMonth() : null, today));
        normalizedFilter.setYear(resolveYear(filter != null ? filter.getYear() : null, today));
        return normalizedFilter;
    }

    private Long normalizeEntityId(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private int resolveMonth(Integer month, LocalDate today) {
        if (month == null || month < 1 || month > 12) {
            return today.getMonthValue();
        }
        return month;
    }

    private int resolveYear(Integer year, LocalDate today) {
        if (year == null || year < 2000 || year > 2100) {
            return today.getYear();
        }
        return year;
    }

    private String normalizeEmployeeStatus(String employeeStatus) {
        if (!StringUtils.hasText(employeeStatus)) {
            return "ACTIVE";
        }
        String normalized = employeeStatus.trim().toUpperCase(Locale.ENGLISH);
        if ("ALL".equals(normalized) || "ACTIVE".equals(normalized) || "RESIGNED".equals(normalized)) {
            return normalized;
        }
        return "ACTIVE";
    }

    private String resolveEmployeeStatusFilter(String employeeStatus) {
        return "ALL".equalsIgnoreCase(employeeStatus) ? null : employeeStatus;
    }

    private String normalizeSearchText(String searchText) {
        return StringUtils.hasText(searchText) ? searchText.trim() : null;
    }

    private InternalAttendanceReportRow buildRow(
            EmployeeEntity employee,
            EmployeeReportingMappingEntity reportingMapping,
            Map<Long, ProjectMst> mappedProjects,
            Map<LocalDate, DailyAttendanceInternalEntity> attendanceByDate,
            List<LeaveApplicationEntity> approvedLeaves,
            List<TourApplicationEntity> approvedTours,
            Set<LocalDate> holidayDates,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today) {
        InternalAttendanceReportRow row = new InternalAttendanceReportRow();
        row.setEmployeeId(employee.getEmployeeId());
        row.setEmployeeCode(defaultText(employee.getEmployeeCode()));
        row.setEmployeeName(defaultText(employee.getFullName()));
        row.setAgencyName(employee.getAgency() != null
                ? defaultText(employee.getAgency().getAgencyName())
                : "-");
        row.setDesignation(employee.getDesignation() != null
                ? defaultText(employee.getDesignation().getDesignationName())
                : "-");
        row.setDepartmentName(employee.getDepartmentRegistration() != null
                ? defaultText(employee.getDepartmentRegistration().getDepartmentName())
                : "-");
        row.setSubDepartmentName(employee.getSubDepartment() != null
                ? defaultText(employee.getSubDepartment().getSubDeptName())
                : "-");
        row.setEmployeeStatus(defaultText(employee.getStatus()));
        row.setRequestId(defaultText(employee.getRequestId()));
        row.setLevelCode(defaultText(employee.getLevelCode()));
        row.setJoiningDate(employee.getJoiningDate());

        ProjectAssignment assignment = resolveProjectAssignment(employee, reportingMapping, mappedProjects);
        row.setProjectId(assignment.projectId());
        row.setProjectName(assignment.projectName());

        Map<Integer, String> dailyStatus = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String statusCode = resolveStatusCode(
                    employee,
                    date,
                    attendanceByDate.get(date),
                    approvedLeaves,
                    approvedTours,
                    holidayDates,
                    today);
            dailyStatus.put(date.getDayOfMonth(), statusCode);
            applyStatusCount(row, statusCode);
        }

        row.setDailyStatus(dailyStatus);
        return row;
    }

    private String resolveStatusCode(
            EmployeeEntity employee,
            LocalDate date,
            DailyAttendanceInternalEntity attendance,
            List<LeaveApplicationEntity> approvedLeaves,
            List<TourApplicationEntity> approvedTours,
            Set<LocalDate> holidayDates,
            LocalDate today) {
        if (employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate())) {
            return "";
        }
        if (employee.getResignationDate() != null && date.isAfter(employee.getResignationDate())) {
            return "";
        }
        if (isCoveredByLeave(approvedLeaves, date)) {
            return "L";
        }
        if (isCoveredByTour(approvedTours, date)) {
            return "T";
        }
        if (attendance != null) {
            return mapStatusToCode(attendance.getStatus(), attendance);
        }
        if (date.isAfter(today)) {
            return "";
        }
        if (holidayDates.contains(date)) {
            return "H";
        }
        if (isWeekend(date)) {
            return "W";
        }
        return "A";
    }

    private boolean isCoveredByLeave(List<LeaveApplicationEntity> approvedLeaves, LocalDate date) {
        return approvedLeaves.stream()
                .anyMatch(leave -> leave.getStartDate() != null
                        && leave.getEndDate() != null
                        && !date.isBefore(leave.getStartDate())
                        && !date.isAfter(leave.getEndDate()));
    }

    private boolean isCoveredByTour(List<TourApplicationEntity> approvedTours, LocalDate date) {
        return approvedTours.stream()
                .anyMatch(tour -> tour.getStartDate() != null
                        && tour.getEndDate() != null
                        && !date.isBefore(tour.getStartDate())
                        && !date.isAfter(tour.getEndDate()));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String mapStatusToCode(String status, DailyAttendanceInternalEntity attendance) {
        if (!StringUtils.hasText(status)) {
            if (attendance != null && (StringUtils.hasText(attendance.getInTime()) || StringUtils.hasText(attendance.getOutTime()))) {
                return "P";
            }
            return "";
        }

        return switch (status.trim().toUpperCase(Locale.ENGLISH)) {
            case "P", "PRESENT" -> "P";
            case "A", "ABSENT" -> "A";
            case "W", "WO", "WEEK_OFF" -> "W";
            case "H", "HOLIDAY" -> "H";
            case "L", "LEAVE" -> "L";
            case "T", "TOUR" -> "T";
            default -> status.trim().toUpperCase(Locale.ENGLISH);
        };
    }

    private void applyStatusCount(InternalAttendanceReportRow row, String statusCode) {
        if (!StringUtils.hasText(statusCode)) {
            return;
        }

        switch (statusCode) {
            case "P" -> row.setPresentCount(row.getPresentCount() + 1);
            case "A" -> row.setAbsentCount(row.getAbsentCount() + 1);
            case "L" -> row.setLeaveCount(row.getLeaveCount() + 1);
            case "H" -> row.setHolidayCount(row.getHolidayCount() + 1);
            case "W" -> row.setWeekOffCount(row.getWeekOffCount() + 1);
            case "T" -> row.setTourCount(row.getTourCount() + 1);
            default -> {
            }
        }
    }

    private InternalAttendanceReportSummary buildSummary(
            List<InternalAttendanceReportRow> rows,
            LocalDate startDate,
            LocalDate endDate,
            Set<LocalDate> holidayDates,
            LocalDate today) {
        LocalDate summaryEndDate = resolveSummaryEndDate(startDate, endDate, today);
        InternalAttendanceReportSummary summary = new InternalAttendanceReportSummary();
        summary.setTotalDaysInMonth(startDate.datesUntil(endDate.plusDays(1)).count());
        summary.setOfficeDayCount(countOfficeDays(startDate, summaryEndDate, holidayDates));
        summary.setTotalHolidayCount(countHolidayDays(startDate, summaryEndDate, holidayDates));
        summary.setTotalWeekOffCount(countWeekOffDays(startDate, summaryEndDate, holidayDates));
        summary.setEmployeeCount(rows.size());
        summary.setPresentCount(rows.stream().mapToLong(InternalAttendanceReportRow::getPresentCount).sum());
        summary.setAbsentCount(rows.stream().mapToLong(InternalAttendanceReportRow::getAbsentCount).sum());
        summary.setLeaveCount(rows.stream().mapToLong(InternalAttendanceReportRow::getLeaveCount).sum());
        summary.setHolidayCount(rows.stream().mapToLong(InternalAttendanceReportRow::getHolidayCount).sum());
        summary.setWeekOffCount(rows.stream().mapToLong(InternalAttendanceReportRow::getWeekOffCount).sum());
        summary.setTourCount(rows.stream().mapToLong(InternalAttendanceReportRow::getTourCount).sum());
        return summary;
    }

    private LocalDate resolveSummaryEndDate(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (today.isBefore(startDate)) {
            return null;
        }
        return today.isBefore(endDate) ? today : endDate;
    }

    private long countOfficeDays(LocalDate startDate, LocalDate endDate, Set<LocalDate> holidayDates) {
        if (endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> !holidayDates.contains(date))
                .filter(date -> !isWeekend(date))
                .count();
    }

    private long countHolidayDays(LocalDate startDate, LocalDate endDate, Set<LocalDate> holidayDates) {
        if (endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(holidayDates::contains)
                .count();
    }

    private long countWeekOffDays(LocalDate startDate, LocalDate endDate, Set<LocalDate> holidayDates) {
        if (endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> !holidayDates.contains(date))
                .filter(this::isWeekend)
                .count();
    }

    private boolean matchesProject(
            EmployeeEntity employee,
            EmployeeReportingMappingEntity reportingMapping,
            Map<Long, ProjectMst> mappedProjects,
            Long projectId) {
        if (projectId == null) {
            return true;
        }
        return projectId.equals(resolveProjectAssignment(employee, reportingMapping, mappedProjects).projectId());
    }

    private boolean matchesSearch(
            EmployeeEntity employee,
            EmployeeReportingMappingEntity reportingMapping,
            Map<Long, ProjectMst> mappedProjects,
            String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return true;
        }

        String normalizedSearch = searchText.toUpperCase(Locale.ENGLISH);
        ProjectAssignment assignment = resolveProjectAssignment(employee, reportingMapping, mappedProjects);

        return Stream.of(
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getAgency() != null ? employee.getAgency().getAgencyName() : null,
                employee.getRequestId(),
                employee.getLevelCode(),
                employee.getStatus(),
                employee.getDepartmentRegistration() != null ? employee.getDepartmentRegistration().getDepartmentName() : null,
                employee.getSubDepartment() != null ? employee.getSubDepartment().getSubDeptName() : null,
                employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null,
                assignment.projectName())
                .filter(StringUtils::hasText)
                .map(value -> value.toUpperCase(Locale.ENGLISH))
                .anyMatch(value -> value.contains(normalizedSearch));
    }

    private ProjectAssignment resolveProjectAssignment(
            EmployeeEntity employee,
            EmployeeReportingMappingEntity reportingMapping,
            Map<Long, ProjectMst> mappedProjects) {
        if (reportingMapping != null && reportingMapping.getProjectId() != null) {
            ProjectMst mappedProject = mappedProjects.get(reportingMapping.getProjectId());
            if (mappedProject != null && StringUtils.hasText(mappedProject.getProjectName())) {
                return new ProjectAssignment(mappedProject.getProjectId(), mappedProject.getProjectName().trim());
            }
            return new ProjectAssignment(reportingMapping.getProjectId(), "-");
        }

        if (employee.getPreOnboarding() != null
                && employee.getPreOnboarding().getInterviewDetail() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst() != null) {
            ProjectMst project = employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst();
            return new ProjectAssignment(
                    project.getProjectId(),
                    StringUtils.hasText(project.getProjectName()) ? project.getProjectName().trim() : "-");
        }

        return new ProjectAssignment(null, "-");
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String normalizeForSort(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ENGLISH) : "";
    }

    private EmployeeReportingMappingEntity pickPreferredReportingMapping(
            EmployeeReportingMappingEntity left,
            EmployeeReportingMappingEntity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        Long leftId = left.getMappingId();
        Long rightId = right.getMappingId();
        if (leftId == null) {
            return right;
        }
        if (rightId == null) {
            return left;
        }
        return rightId >= leftId ? right : left;
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

    private record ProjectAssignment(Long projectId, String projectName) {
    }
}
