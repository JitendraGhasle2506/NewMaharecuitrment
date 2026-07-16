package com.maharecruitment.gov.in.attendance.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClient;
import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClientUnavailableException;
import com.maharecruitment.gov.in.attendance.client.model.InternalAttendanceDayRecord;
import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;
import com.maharecruitment.gov.in.attendance.entity.AttendanceSource;
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
        long syncStartedAtNanos = System.nanoTime();

        if (!properties.isEnabled()) {
            log.debug("Internal attendance API sync is disabled. startDate={}, endDate={}", startDate, endDate);
            return new InternalAttendanceSyncResult(false, startDate, endDate, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0L, elapsedMillis(syncStartedAtNanos), null);
        }

        List<EmployeeEntity> candidates = employeeRepository.findInternalAttendanceSyncCandidates();
        Map<String, List<EmployeeEntity>> employeesByEmployeeCode = new LinkedHashMap<>();
        int skippedEmployees = 0;

        for (EmployeeEntity employee : candidates) {
            if (employee == null || isFutureJoining(employee, endDate)) {
                skippedEmployees++;
                continue;
            }

            String employeeCode;
            try {
                employeeCode = buildEmployeeCode(employee);
            } catch (IllegalArgumentException ex) {
                skippedEmployees++;
                log.warn("Skipping attendance sync for employeeId={} because employee_code cannot be derived. {}",
                        employee.getEmployeeId(),
                        ex.getMessage());
                continue;
            }
            employeesByEmployeeCode.computeIfAbsent(employeeCode, ignored -> new ArrayList<>()).add(employee);
        }

        List<EmployeeSyncTarget> syncTargets = new ArrayList<>();
        List<EmployeeEntity> employeesToUpdate = new ArrayList<>();
        for (Map.Entry<String, List<EmployeeEntity>> entry : employeesByEmployeeCode.entrySet()) {
            List<EmployeeEntity> employees = entry.getValue();
            if (employees.size() > 1) {
                skippedEmployees += employees.size();
                log.warn(
                        "Skipping attendance sync for ambiguous employee_code {} because it maps to multiple employees {}.",
                        entry.getKey(),
                        employees.stream().map(EmployeeEntity::getEmployeeId).toList());
                continue;
            }

            EmployeeEntity employee = employees.get(0);
            Optional<EmployeeEntity> existingEmployeeWithCode = employeeRepository.findByEmployeeCodeIgnoreCase(entry.getKey());
            if (existingEmployeeWithCode.isPresent()
                    && !Objects.equals(existingEmployeeWithCode.get().getEmployeeId(), employee.getEmployeeId())) {
                skippedEmployees++;
                log.warn(
                        "Skipping attendance sync for employeeId={} because derived employee_code {} is already assigned to employeeId={}.",
                        employee.getEmployeeId(),
                        entry.getKey(),
                        existingEmployeeWithCode.get().getEmployeeId());
                continue;
            }

            if (!entry.getKey().equals(normalizeText(employee.getEmployeeCode()))) {
                employee.setEmployeeCode(entry.getKey());
                employeesToUpdate.add(employee);
            }
            syncTargets.add(new EmployeeSyncTarget(entry.getKey(), employee));
        }

        if (!employeesToUpdate.isEmpty()) {
            employeeRepository.saveAll(employeesToUpdate);
            log.info("Updated employee_master.employee_code for {} internal employees before attendance sync.",
                    employeesToUpdate.size());
        }

        List<InternalAttendanceDayRecord> apiRecords;
        long apiStartedAtNanos = System.nanoTime();
        long apiTimeMillis;
        try {
            apiRecords = attendanceReportClient.fetchAttendanceReport(startDate, endDate);
            apiTimeMillis = elapsedMillis(apiStartedAtNanos);
        } catch (InternalAttendanceReportClientUnavailableException ex) {
            apiTimeMillis = elapsedMillis(apiStartedAtNanos);
            log.error("Attendance sync failed because the upstream API is unavailable.", ex);
            int failedEmployees = syncTargets.size();
            return new InternalAttendanceSyncResult(
                    true,
                    startDate,
                    endDate,
                    failedEmployees,
                    0,
                    skippedEmployees,
                    failedEmployees,
                    0,
                    0,
                    0,
                    0,
                    0,
                    apiTimeMillis,
                    elapsedMillis(syncStartedAtNanos),
                    ex.getMessage());
        } catch (Exception ex) {
            apiTimeMillis = elapsedMillis(apiStartedAtNanos);
            log.error("Attendance sync failed while fetching organization attendance.", ex);
            int failedEmployees = syncTargets.isEmpty() ? 0 : syncTargets.size();
            return new InternalAttendanceSyncResult(
                    true,
                    startDate,
                    endDate,
                    failedEmployees,
                    0,
                    skippedEmployees,
                    failedEmployees,
                    0,
                    0,
                    0,
                    0,
                    0,
                    apiTimeMillis,
                    elapsedMillis(syncStartedAtNanos),
                    ex.getMessage());
        }

        if (apiRecords.isEmpty()) {
            log.info("No attendance data found for selected date range. startDate={}, endDate={}", startDate, endDate);
        }

        Map<String, List<InternalAttendanceDayRecord>> apiRecordsByEmployeeCode = apiRecords.stream()
                .filter(record -> StringUtils.hasText(record.getUniqueCode()))
                .collect(Collectors.groupingBy(
                        record -> normalizeEmployeeCode(record.getUniqueCode()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        int attemptedEmployees = 0;
        int syncedEmployees = 0;
        int failedEmployees = 0;
        AttendanceSyncCounters counters = new AttendanceSyncCounters();

        for (int index = 0; index < syncTargets.size(); index++) {
            EmployeeSyncTarget syncTarget = syncTargets.get(index);
            EmployeeEntity employee = syncTarget.employee();
            attemptedEmployees++;
            try {
                List<InternalAttendanceDayRecord> employeeRecords = apiRecordsByEmployeeCode
                        .getOrDefault(normalizeEmployeeCode(syncTarget.employeeCode()), List.of());
                AttendancePersistenceResult persistenceResult =
                        syncEmployeeAttendance(employee, employeeRecords, startDate, endDate);
                syncedEmployees++;
                counters.add(persistenceResult);
            } catch (Exception ex) {
                failedEmployees++;
                log.error("Attendance sync failed for employeeId={}, employeeCode={}",
                        employee.getEmployeeId(),
                        syncTarget.employeeCode(),
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
                counters.upsertedRows(),
                counters.insertedRows(),
                counters.updatedRows(),
                counters.skippedRows(),
                counters.duplicateRows(),
                apiTimeMillis,
                elapsedMillis(syncStartedAtNanos),
                apiRecords.isEmpty() ? "No attendance data found for selected date." : null);
    }

    @Override
    public long countEligibleInternalEmployees() {
        return employeeRepository.countInternalAttendanceSyncCandidates();
    }

    protected AttendancePersistenceResult syncEmployeeAttendance(
            EmployeeEntity employee,
            List<InternalAttendanceDayRecord> apiRecords,
            LocalDate startDate,
            LocalDate endDate) {
        return transactionTemplate.execute(status -> persistEmployeeAttendance(employee, apiRecords, startDate, endDate));
    }

    private AttendancePersistenceResult persistEmployeeAttendance(
            EmployeeEntity employee,
            List<InternalAttendanceDayRecord> apiRecords,
            LocalDate startDate,
            LocalDate endDate) {

        if (apiRecords.isEmpty()) {
            log.info("Attendance API returned no records for employeeId={}, uniqueCode={}, startDate={}, endDate={}",
                    employee.getEmployeeId(),
                    employee.getEmployeeCode(),
                    startDate,
                    endDate);
            return AttendancePersistenceResult.empty();
        }

        List<DailyAttendanceInternalEntity> existingRows = dailyAttendanceInternalRepository
                .findByEmployeeIdAndAttendanceDateBetween(employee.getEmployeeId(), startDate, endDate)
                .stream().toList();
        int duplicateRows = Math.max(existingRows.size()
                - (int) existingRows.stream()
                        .map(DailyAttendanceInternalEntity::getAttendanceDate)
                        .distinct()
                        .count(), 0);

        Map<LocalDate, DailyAttendanceInternalEntity> existingRowsByDate = existingRows.stream()
                .collect(Collectors.toMap(
                        DailyAttendanceInternalEntity::getAttendanceDate,
                        daily -> daily,
                        this::pickLatestPersistedRow,
                        HashMap::new));

        java.util.Set<LocalDate> approvedManualAttendanceDates = manualAttendanceRequestRepository
                .findByUserIdAndAttendanceDateBetweenAndHodStatusIgnoreCase(
                        employee.getEmployeeId(),
                        startDate,
                        endDate,
                        "APPROVED")
                .stream()
                .map(ManualAttendanceRequestEntity::getAttendanceDate)
                .collect(Collectors.toSet());

        List<InternalAttendanceDayRecord> datedApiRecords = apiRecords.stream()
                .filter(record -> record.getAttendanceDate() != null)
                .toList();
        duplicateRows += Math.max(datedApiRecords.size()
                - (int) datedApiRecords.stream()
                        .map(InternalAttendanceDayRecord::getAttendanceDate)
                        .distinct()
                        .count(), 0);

        Map<LocalDate, InternalAttendanceDayRecord> apiRecordByDate = datedApiRecords.stream()
                .collect(Collectors.toMap(
                        InternalAttendanceDayRecord::getAttendanceDate,
                        record -> record,
                        this::pickPreferredApiRecord,
                        LinkedHashMap::new));

        List<DailyAttendanceInternalEntity> entitiesToSave = new ArrayList<>();
        LocalDateTime syncTimestamp = LocalDateTime.now(resolveZoneId());
        int insertedRows = 0;
        int updatedRows = 0;
        int skippedRows = apiRecords.size() - datedApiRecords.size();
        for (InternalAttendanceDayRecord apiRecord : apiRecordByDate.values().stream()
                .sorted(Comparator.comparing(InternalAttendanceDayRecord::getAttendanceDate))
                .toList()) {
            LocalDate attendanceDate = apiRecord.getAttendanceDate();
            if (attendanceDate == null || attendanceDate.isBefore(startDate) || attendanceDate.isAfter(endDate)) {
                skippedRows++;
                continue;
            }
            if (employee.getJoiningDate() != null && attendanceDate.isBefore(employee.getJoiningDate())) {
                skippedRows++;
                continue;
            }
            if (approvedManualAttendanceDates.contains(attendanceDate)) {
                log.debug("Skipping API overwrite for approved manual attendance. employeeId={}, date={}",
                        employee.getEmployeeId(),
                        attendanceDate);
                skippedRows++;
                continue;
            }

            DailyAttendanceInternalEntity existingRow = existingRowsByDate.get(attendanceDate);
            if (isUserManagedAttendance(existingRow)) {
                log.debug("Skipping API overwrite for user-managed attendance. employeeId={}, date={}, source={}",
                        employee.getEmployeeId(),
                        attendanceDate,
                        existingRow.getAttendanceSource());
                skippedRows++;
                continue;
            }

            DailyAttendanceInternalEntity entity = existingRow != null ? existingRow : new DailyAttendanceInternalEntity();
            if (existingRow == null) {
                insertedRows++;
            } else {
                updatedRows++;
            }
            applyApiRecord(entity, employee, apiRecord);
            stampAuditFields(entity, syncTimestamp);
            entitiesToSave.add(entity);
        }

        if (entitiesToSave.isEmpty()) {
            return new AttendancePersistenceResult(0, 0, skippedRows, duplicateRows);
        }

        dailyAttendanceInternalRepository.saveAll(entitiesToSave);
        return new AttendancePersistenceResult(insertedRows, updatedRows, skippedRows, duplicateRows);
    }

    private void applyApiRecord(
            DailyAttendanceInternalEntity entity,
            EmployeeEntity employee,
            InternalAttendanceDayRecord apiRecord) {
        entity.setEmployeeId(employee.getEmployeeId());
        entity.setEmployeeCode(normalizeText(apiRecord.getUniqueCode()));
        entity.setAttendanceDate(apiRecord.getAttendanceDate());
        entity.setAttendanceSource(AttendanceSource.API);
        entity.setApiStatus("Y");
        entity.setMobileAppStatus("N");
        entity.setInTime(normalizeText(apiRecord.getInTime()));
        entity.setOutTime(normalizeText(apiRecord.getOutTime()));
        entity.setTotalHours(calculateTotalHours(apiRecord.getInTime(), apiRecord.getOutTime()));
        entity.setStatus(mapApiStatus(apiRecord.getStatus()));
        entity.setMonth(apiRecord.getAttendanceDate().getMonthValue());
        entity.setYear(apiRecord.getAttendanceDate().getYear());
    }

    private boolean isUserManagedAttendance(DailyAttendanceInternalEntity entity) {
        if (entity == null || entity.getAttendanceSource() == null) {
            return false;
        }
        return entity.getAttendanceSource() == AttendanceSource.MOBILE_APP
                || entity.getAttendanceSource() == AttendanceSource.WEB;
    }

    private void stampAuditFields(DailyAttendanceInternalEntity entity, LocalDateTime syncTimestamp) {
        if (entity.getCreatedDate() == null) {
            entity.setCreatedDate(syncTimestamp);
        }
        entity.setUpdatedDate(syncTimestamp);
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

    private String normalizeEmployeeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String buildEmployeeCode(EmployeeEntity employee) {
        if (employee == null || !StringUtils.hasText(employee.getAadhaarNumber())) {
            throw new IllegalArgumentException("Aadhaar number is missing.");
        }

        String digitsOnly = employee.getAadhaarNumber().replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            throw new IllegalArgumentException("Aadhaar number must contain at least 4 digits.");
        }

        return properties.getUniqueCodePrefix() + digitsOnly.substring(digitsOnly.length() - 4);
    }

    private long elapsedMillis(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    private record EmployeeSyncTarget(String employeeCode, EmployeeEntity employee) {
    }

    protected record AttendancePersistenceResult(
            int insertedRows,
            int updatedRows,
            int skippedRows,
            int duplicateRows) {

        static AttendancePersistenceResult empty() {
            return new AttendancePersistenceResult(0, 0, 0, 0);
        }

        int upsertedRows() {
            return insertedRows + updatedRows;
        }
    }

    private static final class AttendanceSyncCounters {

        private int insertedRows;
        private int updatedRows;
        private int skippedRows;
        private int duplicateRows;

        void add(AttendancePersistenceResult result) {
            if (result == null) {
                return;
            }
            insertedRows += result.insertedRows();
            updatedRows += result.updatedRows();
            skippedRows += result.skippedRows();
            duplicateRows += result.duplicateRows();
        }

        int upsertedRows() {
            return insertedRows + updatedRows;
        }

        int insertedRows() {
            return insertedRows;
        }

        int updatedRows() {
            return updatedRows;
        }

        int skippedRows() {
            return skippedRows;
        }

        int duplicateRows() {
            return duplicateRows;
        }
    }
}
