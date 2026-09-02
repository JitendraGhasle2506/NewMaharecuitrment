package com.maharecruitment.gov.in.attendance.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.entity.ManualAttendanceRequestEntity;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.LeaveApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.repository.TourApplicationRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.attendance.service.AttendanceEventTimeResolver;
import com.maharecruitment.gov.in.attendance.service.AttendanceStatusResolver;
import com.maharecruitment.gov.in.attendance.service.TeamAttendanceService;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceMemberView;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceOverview;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeCellMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Service
@Transactional(readOnly = true)
public class TeamAttendanceServiceImpl implements TeamAttendanceService {

    private static final String NOT_AVAILABLE = "-";

    private final ReportingManagerService reportingManagerService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCellMappingRepository employeeCellMappingRepository;
    private final EmployeeReportingMappingRepository reportingMappingRepository;
    private final ProjectMstRepository projectRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceRepository;
    private final HolidayRepository holidayRepository;
    private final WeekOffWorkingDayRepository workingDayRepository;
    private final LeaveApplicationRepository leaveRepository;
    private final TourApplicationRepository tourRepository;
    private final ManualAttendanceRequestRepository manualAttendanceRepository;

    public TeamAttendanceServiceImpl(
            ReportingManagerService reportingManagerService,
            EmployeeRepository employeeRepository,
            EmployeeCellMappingRepository employeeCellMappingRepository,
            EmployeeReportingMappingRepository reportingMappingRepository,
            ProjectMstRepository projectRepository,
            DailyAttendanceInternalRepository dailyAttendanceRepository,
            HolidayRepository holidayRepository,
            WeekOffWorkingDayRepository workingDayRepository,
            LeaveApplicationRepository leaveRepository,
            TourApplicationRepository tourRepository,
            ManualAttendanceRequestRepository manualAttendanceRepository) {
        this.reportingManagerService = reportingManagerService;
        this.employeeRepository = employeeRepository;
        this.employeeCellMappingRepository = employeeCellMappingRepository;
        this.reportingMappingRepository = reportingMappingRepository;
        this.projectRepository = projectRepository;
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.holidayRepository = holidayRepository;
        this.workingDayRepository = workingDayRepository;
        this.leaveRepository = leaveRepository;
        this.tourRepository = tourRepository;
        this.manualAttendanceRepository = manualAttendanceRepository;
    }

    @Override
    public TeamAttendanceOverview getOverview(Long authorityUserId, YearMonth period) {
        Objects.requireNonNull(period, "Attendance period is required");
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(authorityUserId);
        return buildOverview(employeeIds, period);
    }

    private TeamAttendanceOverview buildOverview(List<Long> employeeIds, YearMonth period) {
        if (employeeIds.isEmpty()) {
            return emptyOverview(period);
        }

        List<EmployeeEntity> employees = employeeRepository
                .findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(employeeIds)
                .stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .filter(employee -> "INTERNAL".equalsIgnoreCase(employee.getRecruitmentType()))
                .toList();
        if (employees.isEmpty()) {
            return emptyOverview(period);
        }

        List<Long> activeEmployeeIds = employees.stream().map(EmployeeEntity::getEmployeeId).toList();
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate statusDate = startDate.isAfter(today) ? null : min(endDate, today);

        Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> attendanceByEmployee =
                groupAttendance(activeEmployeeIds, startDate, endDate);
        Set<LocalDate> holidayDates = holidayRepository.findByHolidayDateBetween(startDate, endDate).stream()
                .map(holiday -> holiday.getHolidayDate())
                .collect(Collectors.toSet());
        Set<LocalDate> workingDayOverrides = workingDayRepository.findByWorkingDateBetween(startDate, endDate).stream()
                .map(workingDay -> workingDay.getWorkingDate())
                .collect(Collectors.toSet());
        Map<Long, List<LeaveApplicationEntity>> leavesByEmployee = leaveRepository
                .findApprovedOverlappingPeriod(activeEmployeeIds, startDate, endDate).stream()
                .collect(Collectors.groupingBy(LeaveApplicationEntity::getEmployeeId));
        Map<Long, List<TourApplicationEntity>> toursByEmployee = tourRepository
                .findApprovedOverlappingPeriod(activeEmployeeIds, startDate, endDate).stream()
                .collect(Collectors.groupingBy(TourApplicationEntity::getEmployeeId));
        Map<Long, Set<LocalDate>> pendingDatesByEmployee = manualAttendanceRepository
                .findByUserIdInAndAttendanceDateBetween(activeEmployeeIds, startDate, endDate).stream()
                .filter(request -> "PENDING".equalsIgnoreCase(request.getHodStatus()))
                .collect(Collectors.groupingBy(
                        ManualAttendanceRequestEntity::getUserId,
                        Collectors.mapping(ManualAttendanceRequestEntity::getAttendanceDate, Collectors.toSet())));
        Map<Long, String> projectNamesByEmployee = loadProjectNames(activeEmployeeIds);
        Map<Long, String> unitNamesByEmployee = loadUnitNames(activeEmployeeIds);

        List<TeamAttendanceMemberView> members = new ArrayList<>(employees.size());
        for (EmployeeEntity employee : employees) {
            members.add(toMemberView(
                    employee,
                    startDate,
                    endDate,
                    today,
                    statusDate,
                    attendanceByEmployee.getOrDefault(employee.getEmployeeId(), Map.of()),
                    leavesByEmployee.getOrDefault(employee.getEmployeeId(), List.of()),
                    toursByEmployee.getOrDefault(employee.getEmployeeId(), List.of()),
                    pendingDatesByEmployee.getOrDefault(employee.getEmployeeId(), Set.of()),
                    holidayDates,
                    workingDayOverrides,
                    projectNamesByEmployee.get(employee.getEmployeeId()),
                    unitNamesByEmployee.get(employee.getEmployeeId())));
        }

        long presentDays = members.stream().mapToLong(TeamAttendanceMemberView::presentDays).sum();
        long absentDays = members.stream().mapToLong(TeamAttendanceMemberView::absentDays).sum();
        long leaveDays = members.stream().mapToLong(TeamAttendanceMemberView::leaveDays).sum();
        long tourDays = members.stream().mapToLong(TeamAttendanceMemberView::tourDays).sum();
        DailyStatusCounts todayCounts = YearMonth.from(today).equals(period)
                ? summarizeDailyStatuses(
                        employees,
                        today,
                        attendanceByEmployee,
                        leavesByEmployee,
                        toursByEmployee,
                        pendingDatesByEmployee,
                        holidayDates,
                        workingDayOverrides)
                : loadTodayStatusCounts(employees, activeEmployeeIds, today);
        return new TeamAttendanceOverview(
                period,
                statusDate,
                List.copyOf(members),
                todayCounts.present(),
                todayCounts.absent(),
                todayCounts.leave(),
                presentDays,
                absentDays,
                leaveDays,
                tourDays,
                rate(presentDays, presentDays + absentDays + leaveDays + tourDays));
    }

    private DailyStatusCounts loadTodayStatusCounts(
            List<EmployeeEntity> employees,
            List<Long> employeeIds,
            LocalDate today) {
        Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> attendanceByEmployee =
                groupAttendance(employeeIds, today, today);
        Set<LocalDate> holidayDates = holidayRepository.findByHolidayDate(today)
                .map(holiday -> Set.of(holiday.getHolidayDate()))
                .orElseGet(Set::of);
        Set<LocalDate> workingDayOverrides = workingDayRepository.findByWorkingDate(today)
                .map(workingDay -> Set.of(workingDay.getWorkingDate()))
                .orElseGet(Set::of);
        Map<Long, List<LeaveApplicationEntity>> leavesByEmployee = leaveRepository
                .findApprovedOverlappingPeriod(employeeIds, today, today).stream()
                .collect(Collectors.groupingBy(LeaveApplicationEntity::getEmployeeId));
        Map<Long, List<TourApplicationEntity>> toursByEmployee = tourRepository
                .findApprovedOverlappingPeriod(employeeIds, today, today).stream()
                .collect(Collectors.groupingBy(TourApplicationEntity::getEmployeeId));
        Map<Long, Set<LocalDate>> pendingDatesByEmployee = manualAttendanceRepository
                .findByUserIdInAndAttendanceDateBetween(employeeIds, today, today).stream()
                .filter(request -> "PENDING".equalsIgnoreCase(request.getHodStatus()))
                .collect(Collectors.groupingBy(
                        ManualAttendanceRequestEntity::getUserId,
                        Collectors.mapping(ManualAttendanceRequestEntity::getAttendanceDate, Collectors.toSet())));
        return summarizeDailyStatuses(
                employees,
                today,
                attendanceByEmployee,
                leavesByEmployee,
                toursByEmployee,
                pendingDatesByEmployee,
                holidayDates,
                workingDayOverrides);
    }

    private DailyStatusCounts summarizeDailyStatuses(
            List<EmployeeEntity> employees,
            LocalDate date,
            Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> attendanceByEmployee,
            Map<Long, List<LeaveApplicationEntity>> leavesByEmployee,
            Map<Long, List<TourApplicationEntity>> toursByEmployee,
            Map<Long, Set<LocalDate>> pendingDatesByEmployee,
            Set<LocalDate> holidayDates,
            Set<LocalDate> workingDayOverrides) {
        long present = 0;
        long absent = 0;
        long leave = 0;
        for (EmployeeEntity employee : employees) {
            if (employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate())) {
                continue;
            }
            Long employeeId = employee.getEmployeeId();
            String status = resolveStatus(
                    date,
                    attendanceByEmployee.getOrDefault(employeeId, Map.of()).get(date),
                    leavesByEmployee.getOrDefault(employeeId, List.of()),
                    toursByEmployee.getOrDefault(employeeId, List.of()),
                    pendingDatesByEmployee.getOrDefault(employeeId, Set.of()),
                    holidayDates,
                    workingDayOverrides);
            switch (status) {
                case "PRESENT" -> present++;
                case "ABSENT" -> absent++;
                case "LEAVE", "COMP_OFF" -> leave++;
                default -> {
                    // Tours, holidays, week-offs and pending requests remain separate statuses.
                }
            }
        }
        return new DailyStatusCounts(present, absent, leave);
    }

    @Override
    public Optional<TeamAttendanceMemberView> getAuthorizedMember(
            Long authorityUserId,
            Long employeeId,
            YearMonth period) {
        if (employeeId == null) {
            return Optional.empty();
        }
        List<Long> authorizedEmployeeIds = reportingManagerService
                .getEffectiveEmployeeIdsForAuthority(authorityUserId);
        if (!authorizedEmployeeIds.contains(employeeId)) {
            return Optional.empty();
        }
        return buildOverview(List.of(employeeId), period).members().stream().findFirst();
    }

    private TeamAttendanceMemberView toMemberView(
            EmployeeEntity employee,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today,
            LocalDate statusDate,
            Map<LocalDate, DailyAttendanceInternalEntity> attendanceByDate,
            List<LeaveApplicationEntity> leaves,
            List<TourApplicationEntity> tours,
            Set<LocalDate> pendingDates,
            Set<LocalDate> holidayDates,
            Set<LocalDate> workingDayOverrides,
            String projectName,
            String unitName) {
        Map<String, Long> counts = new HashMap<>();
        if (!startDate.isAfter(today)) {
            LocalDate applicableEnd = min(endDate, today);
            for (LocalDate date = startDate; !date.isAfter(applicableEnd); date = date.plusDays(1)) {
                if (employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate())) {
                    continue;
                }
                String status = resolveStatus(
                        date,
                        attendanceByDate.get(date),
                        leaves,
                        tours,
                        pendingDates,
                        holidayDates,
                        workingDayOverrides);
                counts.merge(status, 1L, Long::sum);
            }
        }

        DailyAttendanceInternalEntity latestAttendance = statusDate == null ? null : attendanceByDate.get(statusDate);
        String latestStatus = statusDate == null
                ? "FUTURE"
                : resolveStatus(
                        statusDate,
                        latestAttendance,
                        leaves,
                        tours,
                        pendingDates,
                        holidayDates,
                        workingDayOverrides);
        AttendanceEventTimeResolver.AttendanceEventWindow latestWindow =
                AttendanceEventTimeResolver.resolve(latestAttendance);
        long presentDays = counts.getOrDefault("PRESENT", 0L);
        long absentDays = counts.getOrDefault("ABSENT", 0L);
        long leaveDays = counts.getOrDefault("LEAVE", 0L) + counts.getOrDefault("COMP_OFF", 0L);
        long tourDays = counts.getOrDefault("TOUR", 0L);
        long consideredDays = presentDays + absentDays + leaveDays + tourDays;

        return new TeamAttendanceMemberView(
                employee.getEmployeeId(),
                fallback(employee.getEmployeeCode()),
                fallback(employee.getFullName()),
                initials(employee.getFullName()),
                employee.getDesignation() == null
                        ? NOT_AVAILABLE
                        : fallback(employee.getDesignation().getDesignationName()),
                StringUtils.hasText(unitName)
                        ? unitName
                        : employee.getSubDepartment() == null
                                ? NOT_AVAILABLE
                                : fallback(employee.getSubDepartment().getSubDeptName()),
                fallback(projectName),
                latestStatus,
                fallback(AttendanceEventTimeResolver.format(latestWindow.inTime())),
                fallback(AttendanceEventTimeResolver.format(latestWindow.outTime())),
                presentDays,
                absentDays,
                leaveDays,
                tourDays,
                counts.getOrDefault("HOLIDAY", 0L),
                counts.getOrDefault("WEEK_OFF", 0L),
                rate(presentDays, consideredDays));
    }

    private Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> groupAttendance(
            Collection<Long> employeeIds,
            LocalDate startDate,
            LocalDate endDate) {
        Map<Long, Map<LocalDate, DailyAttendanceInternalEntity>> grouped = new HashMap<>();
        for (DailyAttendanceInternalEntity attendance : dailyAttendanceRepository
                .findByEmployeeIdInAndAttendanceDateBetween(employeeIds, startDate, endDate)) {
            grouped.computeIfAbsent(attendance.getEmployeeId(), ignored -> new HashMap<>())
                    .merge(attendance.getAttendanceDate(), attendance, this::preferAttendanceRow);
        }
        return grouped;
    }

    private Map<Long, String> loadProjectNames(List<Long> employeeIds) {
        Map<Long, EmployeeReportingMappingEntity> mappingsByEmployee = reportingMappingRepository
                .findByEmployeeIdIn(employeeIds).stream()
                .collect(Collectors.toMap(
                        EmployeeReportingMappingEntity::getEmployeeId,
                        Function.identity(),
                        this::latestMapping,
                        LinkedHashMap::new));
        Set<Long> projectIds = mappingsByEmployee.values().stream()
                .map(EmployeeReportingMappingEntity::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> projectsById = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(ProjectMst::getProjectId, ProjectMst::getProjectName));
        return mappingsByEmployee.values().stream()
                .filter(mapping -> mapping.getProjectId() != null)
                .filter(mapping -> projectsById.containsKey(mapping.getProjectId()))
                .collect(Collectors.toMap(
                        EmployeeReportingMappingEntity::getEmployeeId,
                        mapping -> projectsById.get(mapping.getProjectId())));
    }

    private Map<Long, String> loadUnitNames(List<Long> employeeIds) {
        return employeeCellMappingRepository
                .findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(employeeIds).stream()
                .filter(mapping -> mapping.getEmployee() != null && mapping.getCell() != null)
                .filter(mapping -> StringUtils.hasText(mapping.getCell().getCellName()))
                .collect(Collectors.toMap(
                        mapping -> mapping.getEmployee().getEmployeeId(),
                        mapping -> mapping.getCell().getCellName().trim(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private String resolveStatus(
            LocalDate date,
            DailyAttendanceInternalEntity daily,
            List<LeaveApplicationEntity> leaves,
            List<TourApplicationEntity> tours,
            Set<LocalDate> pendingDates,
            Set<LocalDate> holidayDates,
            Set<LocalDate> workingDayOverrides) {
        boolean completePunch = hasCompletePunch(daily);
        LeaveApplicationEntity leave = completePunch ? null : leaves.stream()
                .filter(item -> covers(item.getStartDate(), item.getEndDate(), date))
                .findFirst()
                .orElse(null);
        if (leave != null) {
            return isCompOff(leave) ? "COMP_OFF" : "LEAVE";
        }
        if (tours.stream().anyMatch(item -> covers(item.getStartDate(), item.getEndDate(), date))) {
            return "TOUR";
        }
        if (holidayDates.contains(date)) {
            return "HOLIDAY";
        }
        String resolved = AttendanceStatusResolver.resolveDisplayStatus(daily);
        if (workingDayOverrides.contains(date) && "WEEK_OFF".equals(resolved)) {
            resolved = null;
        }
        if (StringUtils.hasText(resolved)) {
            return resolved;
        }
        if (pendingDates.contains(date)) {
            return "PENDING";
        }
        return isWeekOff(date, workingDayOverrides) ? "WEEK_OFF" : "ABSENT";
    }

    private DailyAttendanceInternalEntity preferAttendanceRow(
            DailyAttendanceInternalEntity first,
            DailyAttendanceInternalEntity second) {
        int firstScore = attendanceScore(first);
        int secondScore = attendanceScore(second);
        if (firstScore != secondScore) {
            return secondScore > firstScore ? second : first;
        }
        long firstId = first.getId() == null ? Long.MIN_VALUE : first.getId();
        long secondId = second.getId() == null ? Long.MIN_VALUE : second.getId();
        return secondId > firstId ? second : first;
    }

    private int attendanceScore(DailyAttendanceInternalEntity attendance) {
        AttendanceEventTimeResolver.AttendanceEventWindow window = AttendanceEventTimeResolver.resolve(attendance);
        int score = 0;
        if (window.inTime() != null) {
            score += 2;
        }
        if (window.outTime() != null) {
            score += 2;
        }
        if (StringUtils.hasText(AttendanceStatusResolver.resolveDisplayStatus(attendance))) {
            score++;
        }
        return score;
    }

    private EmployeeReportingMappingEntity latestMapping(
            EmployeeReportingMappingEntity first,
            EmployeeReportingMappingEntity second) {
        long firstId = first.getMappingId() == null ? Long.MIN_VALUE : first.getMappingId();
        long secondId = second.getMappingId() == null ? Long.MIN_VALUE : second.getMappingId();
        return secondId > firstId ? second : first;
    }

    private boolean hasCompletePunch(DailyAttendanceInternalEntity attendance) {
        AttendanceEventTimeResolver.AttendanceEventWindow window = AttendanceEventTimeResolver.resolve(attendance);
        return window.inTime() != null && window.outTime() != null;
    }

    private boolean covers(LocalDate startDate, LocalDate endDate, LocalDate date) {
        return startDate != null && endDate != null
                && !date.isBefore(startDate)
                && !date.isAfter(endDate);
    }

    private boolean isCompOff(LeaveApplicationEntity leave) {
        if (leave.getCompOffWorkDate() != null) {
            return true;
        }
        String leaveType = leave.getLeaveType();
        if (!StringUtils.hasText(leaveType)) {
            return false;
        }
        String normalized = leaveType.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return Set.of("CO", "COMPOFF", "COMPOFFLEAVE", "COMPENSATORYOFF", "COMPENSATORYLEAVE")
                .contains(normalized);
    }

    private boolean isWeekOff(LocalDate date, Set<LocalDate> workingDayOverrides) {
        return (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                && !workingDayOverrides.contains(date);
    }

    private TeamAttendanceOverview emptyOverview(YearMonth period) {
        LocalDate today = LocalDate.now();
        LocalDate statusDate = period.atDay(1).isAfter(today) ? null : min(period.atEndOfMonth(), today);
        return new TeamAttendanceOverview(period, statusDate, List.of(), 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private int rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : (int) Math.round((numerator * 100.0) / denominator);
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value.trim() : NOT_AVAILABLE;
    }

    private String initials(String name) {
        if (!StringUtils.hasText(name)) {
            return "NA";
        }
        String[] words = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder(2);
        for (String word : words) {
            if (!word.isBlank()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (result.length() == 2) {
                    break;
                }
            }
        }
        return result.toString();
    }

    private record DailyStatusCounts(long present, long absent, long leave) {
    }
}
