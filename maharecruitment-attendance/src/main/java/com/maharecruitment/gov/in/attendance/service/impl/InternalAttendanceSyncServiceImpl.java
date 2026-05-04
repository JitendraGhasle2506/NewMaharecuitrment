package com.maharecruitment.gov.in.attendance.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClient;
import com.maharecruitment.gov.in.attendance.client.model.InternalAttendanceDayRecord;
import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.ManualAttendanceRequestEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.service.InternalAttendanceSyncService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class InternalAttendanceSyncServiceImpl implements InternalAttendanceSyncService {

    private static final Logger log = LoggerFactory.getLogger(InternalAttendanceSyncServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final DailyAttendanceInternalRepository dailyAttendanceInternalRepository;
    private final ManualAttendanceRequestRepository manualAttendanceRequestRepository;
    private final InternalAttendanceReportClient attendanceReportClient;
    private final InternalAttendanceSyncProperties properties;
    private final TransactionTemplate transactionTemplate;

    public InternalAttendanceSyncServiceImpl(
            EmployeeRepository employeeRepository,
            DailyAttendanceInternalRepository dailyAttendanceInternalRepository,
            ManualAttendanceRequestRepository manualAttendanceRequestRepository,
            InternalAttendanceReportClient attendanceReportClient,
            InternalAttendanceSyncProperties properties,
            TransactionTemplate transactionTemplate) {
        this.employeeRepository = employeeRepository;
        this.dailyAttendanceInternalRepository = dailyAttendanceInternalRepository;
        this.manualAttendanceRequestRepository = manualAttendanceRequestRepository;
        this.attendanceReportClient = attendanceReportClient;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public InternalAttendanceSyncResult syncScheduledAttendance() {
        LocalDate today = LocalDate.now(resolveZoneId());
        LocalDate startDate = resolveSyncStartDate(today);
        LocalDate endDate = resolveSyncEndDate(today);
        return syncAttendance(startDate, endDate);
    }

    @Override
    public InternalAttendanceSyncResult syncAttendance(LocalDate startDate, LocalDate endDate) {
        validateSyncRange(startDate, endDate);

        if (!properties.isEnabled()) {
            log.debug("Internal attendance API sync is disabled. startDate={}, endDate={}", startDate, endDate);
            return new InternalAttendanceSyncResult(false, startDate, endDate, 0, 0, 0, 0, 0);
        }

        List<EmployeeEntity> candidates = employeeRepository.findInternalAttendanceSyncCandidates();
        Map<String, List<EmployeeEntity>> employeesByUniqueCode = new LinkedHashMap<>();
        int skippedEmployees = 0;

        for (EmployeeEntity employee : candidates) {
            if (employee == null || isFutureJoining(employee, endDate)) {
                skippedEmployees++;
                continue;
            }

            try {
                String uniqueCode = buildUniqueCode(employee);
                employeesByUniqueCode.computeIfAbsent(uniqueCode, ignored -> new ArrayList<>()).add(employee);
            } catch (IllegalArgumentException ex) {
                skippedEmployees++;
                log.warn("Skipping attendance sync for employeeId={} because the external unique code is invalid. {}",
                        employee != null ? employee.getEmployeeId() : null,
                        ex.getMessage());
            }
        }

        int attemptedEmployees = 0;
        int syncedEmployees = 0;
        int failedEmployees = 0;
        int upsertedRows = 0;

        for (Map.Entry<String, List<EmployeeEntity>> entry : employeesByUniqueCode.entrySet()) {
            List<EmployeeEntity> employees = entry.getValue();
            if (employees.size() > 1) {
                skippedEmployees += employees.size();
                log.warn(
                        "Skipping attendance sync for ambiguous unique code {} because it maps to multiple employees {}.",
                        entry.getKey(),
                        employees.stream().map(EmployeeEntity::getEmployeeId).toList());
                continue;
            }

            EmployeeEntity employee = employees.get(0);
            attemptedEmployees++;
            try {
                int savedRows = syncEmployeeAttendance(employee, entry.getKey(), startDate, endDate);
                syncedEmployees++;
                upsertedRows += savedRows;
            } catch (Exception ex) {
                failedEmployees++;
                log.error("Attendance sync failed for employeeId={}, uniqueCode={}",
                        employee.getEmployeeId(),
                        entry.getKey(),
                        ex);
            }
        }

        return new InternalAttendanceSyncResult(
                true,
                startDate,
                endDate,
                attemptedEmployees,
                syncedEmployees,
                skippedEmployees,
                failedEmployees,
                upsertedRows);
    }

    @Override
    public long countEligibleInternalEmployees() {
        return employeeRepository.countInternalAttendanceSyncCandidates();
    }

    protected int syncEmployeeAttendance(
            EmployeeEntity employee,
            String uniqueCode,
            LocalDate startDate,
            LocalDate endDate) {
        List<InternalAttendanceDayRecord> apiRecords = attendanceReportClient.fetchAttendanceReport(
                uniqueCode,
                startDate,
                endDate);

        return transactionTemplate.execute(status -> persistEmployeeAttendance(employee, apiRecords, startDate, endDate));
    }

    private int persistEmployeeAttendance(
            EmployeeEntity employee,
            List<InternalAttendanceDayRecord> apiRecords,
            LocalDate startDate,
            LocalDate endDate) {

        if (apiRecords.isEmpty()) {
            log.info("Attendance API returned no records for employeeId={}, uniqueCode={}, startDate={}, endDate={}",
                    employee.getEmployeeId(),
                    buildUniqueCode(employee),
                    startDate,
                    endDate);
            return 0;
        }

        Map<LocalDate, DailyAttendanceInternalEntity> existingRowsByDate = dailyAttendanceInternalRepository
                .findByEmployeeIdAndAttendanceDateBetween(employee.getEmployeeId(), startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        DailyAttendanceInternalEntity::getAttendanceDate,
                        daily -> daily,
                        this::pickLatestPersistedRow,
                        HashMap::new));

        Set<LocalDate> approvedManualAttendanceDates = manualAttendanceRequestRepository
                .findByUserIdAndAttendanceDateBetweenAndHodStatusIgnoreCase(
                        employee.getEmployeeId(),
                        startDate,
                        endDate,
                        "APPROVED")
                .stream()
                .map(ManualAttendanceRequestEntity::getAttendanceDate)
                .collect(Collectors.toSet());

        Map<LocalDate, InternalAttendanceDayRecord> apiRecordByDate = apiRecords.stream()
                .filter(record -> record.getAttendanceDate() != null)
                .collect(Collectors.toMap(
                        InternalAttendanceDayRecord::getAttendanceDate,
                        record -> record,
                        this::pickPreferredApiRecord,
                        LinkedHashMap::new));

        List<DailyAttendanceInternalEntity> entitiesToSave = new ArrayList<>();
        for (InternalAttendanceDayRecord apiRecord : apiRecordByDate.values().stream()
                .sorted(Comparator.comparing(InternalAttendanceDayRecord::getAttendanceDate))
                .toList()) {
            LocalDate attendanceDate = apiRecord.getAttendanceDate();
            if (attendanceDate == null || attendanceDate.isBefore(startDate) || attendanceDate.isAfter(endDate)) {
                continue;
            }
            if (employee.getJoiningDate() != null && attendanceDate.isBefore(employee.getJoiningDate())) {
                continue;
            }
            if (approvedManualAttendanceDates.contains(attendanceDate)) {
                log.debug("Skipping API overwrite for approved manual attendance. employeeId={}, date={}",
                        employee.getEmployeeId(),
                        attendanceDate);
                continue;
            }

            DailyAttendanceInternalEntity entity = existingRowsByDate.getOrDefault(
                    attendanceDate,
                    new DailyAttendanceInternalEntity());
            applyApiRecord(entity, employee, apiRecord);
            entitiesToSave.add(entity);
        }

        if (entitiesToSave.isEmpty()) {
            return 0;
        }

        dailyAttendanceInternalRepository.saveAll(entitiesToSave);
        return entitiesToSave.size();
    }

    private void applyApiRecord(
            DailyAttendanceInternalEntity entity,
            EmployeeEntity employee,
            InternalAttendanceDayRecord apiRecord) {
        entity.setEmployeeId(employee.getEmployeeId());
        entity.setAttendanceDate(apiRecord.getAttendanceDate());
        entity.setInTime(normalizeText(apiRecord.getInTime()));
        entity.setOutTime(normalizeText(apiRecord.getOutTime()));
        entity.setTotalHours(calculateTotalHours(apiRecord.getInTime(), apiRecord.getOutTime()));
        entity.setStatus(mapApiStatus(apiRecord.getStatus()));
        entity.setMonth(apiRecord.getAttendanceDate().getMonthValue());
        entity.setYear(apiRecord.getAttendanceDate().getYear());
    }

    private DailyAttendanceInternalEntity pickLatestPersistedRow(
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
        return leftId >= rightId ? left : right;
    }

    private InternalAttendanceDayRecord pickPreferredApiRecord(
            InternalAttendanceDayRecord left,
            InternalAttendanceDayRecord right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        int leftScore = scoreApiRecord(left);
        int rightScore = scoreApiRecord(right);
        return rightScore >= leftScore ? right : left;
    }

    private int scoreApiRecord(InternalAttendanceDayRecord record) {
        int score = 0;
        if (StringUtils.hasText(record.getInTime())) {
            score += 1;
        }
        if (StringUtils.hasText(record.getOutTime())) {
            score += 1;
        }
        if (StringUtils.hasText(record.getStatus())) {
            score += 1;
        }
        return score;
    }

    private String buildUniqueCode(EmployeeEntity employee) {
        if (employee == null || !StringUtils.hasText(employee.getAadhaarNumber())) {
            throw new IllegalArgumentException("Aadhaar number is missing.");
        }

        String digitsOnly = employee.getAadhaarNumber().replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            throw new IllegalArgumentException("Aadhaar number must contain at least 4 digits.");
        }

        return properties.getUniqueCodePrefix() + digitsOnly.substring(digitsOnly.length() - 4);
    }

    private boolean isFutureJoining(EmployeeEntity employee, LocalDate endDate) {
        return employee != null
                && employee.getJoiningDate() != null
                && employee.getJoiningDate().isAfter(endDate);
    }

    private LocalDate resolveSyncStartDate(LocalDate today) {
        if (properties.isCurrentDateOnly()) {
            return today;
        }
        return properties.getOverrideStartDate() != null
                ? properties.getOverrideStartDate()
                : YearMonth.from(today).atDay(1);
    }

    private LocalDate resolveSyncEndDate(LocalDate today) {
        if (properties.isCurrentDateOnly()) {
            return today;
        }
        return properties.getOverrideEndDate() != null
                ? properties.getOverrideEndDate()
                : today;
    }

    private void validateSyncRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Attendance sync range could not be resolved.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Attendance sync end date cannot be before the start date.");
        }
    }

    private ZoneId resolveZoneId() {
        return ZoneId.of(properties.getSchedulerZone());
    }

    private String mapApiStatus(String rawStatus) {
        String normalized = normalizeText(rawStatus);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        String status = normalized.toUpperCase();
        switch (status) {
            case "P":
            case "PRESENT":
                return "PRESENT";
            case "A":
            case "ABSENT":
                return "ABSENT";
            case "WO":
            case "W":
            case "WEEK_OFF":
                return "WEEK_OFF";
            case "H":
            case "HOLIDAY":
                return "HOLIDAY";
            case "L":
            case "LEAVE":
                return "LEAVE";
            case "T":
            case "TOUR":
                return "TOUR";
            default:
                return status;
        }
    }

    private String calculateTotalHours(String inTime, String outTime) {
        String normalizedInTime = normalizeText(inTime);
        String normalizedOutTime = normalizeText(outTime);
        if (!StringUtils.hasText(normalizedInTime) || !StringUtils.hasText(normalizedOutTime)) {
            return null;
        }

        try {
            LocalTime in = LocalTime.parse(normalizedInTime);
            LocalTime out = LocalTime.parse(normalizedOutTime);
            if (out.isBefore(in)) {
                return null;
            }

            Duration duration = Duration.between(in, out);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            return "%02d:%02d".formatted(hours, minutes);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
