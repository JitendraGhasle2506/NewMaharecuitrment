package com.maharecruitment.gov.in.attendance.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
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
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.entity.ManualAttendanceRequestEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.service.AttendanceEventTimeResolver;
import com.maharecruitment.gov.in.attendance.service.AttendanceEventTimeResolver.AttendanceEventWindow;
import com.maharecruitment.gov.in.attendance.service.InternalAttendanceSyncService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class InternalAttendanceSyncServiceImpl implements InternalAttendanceSyncService {

    private static final Logger log = LoggerFactory.getLogger(InternalAttendanceSyncServiceImpl.class);
    private static final String PRESENT = "PRESENT";

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

            if (!Objects.equals(entry.getKey(), normalizeText(employee.getEmployeeCode()))) {
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
                .findByEmployeeIdentityAndAttendanceDateBetweenForUpdate(
                        employee.getEmployeeId(),
                        normalizeText(employee.getEmployeeCode()),
                        startDate,
                        endDate)
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
                        this::mergeApiRecords,
                        LinkedHashMap::new));

        List<DailyAttendanceInternalEntity> entitiesToSave = new ArrayList<>();
        List<AttendanceUpdateLogEntry> updateLogEntries = new ArrayList<>();
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
            if (!hasValidApiAttendanceData(apiRecord)) {
                if (existingRow == null) {
                    log.info(
                            "Attendance update skipped. employeeId={}, employeeCode={}, attendanceDate={}, sourceType=API, updatedFields=[], result=NO_VALID_API_DATA",
                            employee.getEmployeeId(),
                            employee.getEmployeeCode(),
                            attendanceDate);
                    skippedRows++;
                    continue;
                }
                existingRow.setApiStatus("N");
                stampAuditFields(existingRow, syncTimestamp);
                entitiesToSave.add(existingRow);
                updateLogEntries.add(new AttendanceUpdateLogEntry(
                        existingRow.getEmployeeId(),
                        existingRow.getEmployeeCode(),
                        attendanceDate,
                        "API",
                        List.of("api_status"),
                        "UPDATED_NO_VALID_API_DATA"));
                updatedRows++;
                continue;
            }

            DailyAttendanceInternalEntity entity = existingRow != null ? existingRow : new DailyAttendanceInternalEntity();
            String result = existingRow == null ? "INSERTED" : "UPDATED";
            if (existingRow == null) {
                insertedRows++;
            } else {
                updatedRows++;
            }
            List<String> updatedFields = applyApiRecord(entity, employee, apiRecord);
            stampAuditFields(entity, syncTimestamp);
            entitiesToSave.add(entity);
            updateLogEntries.add(new AttendanceUpdateLogEntry(
                    entity.getEmployeeId(),
                    entity.getEmployeeCode(),
                    attendanceDate,
                    "API",
                    updatedFields,
                    result));
        }

        if (entitiesToSave.isEmpty()) {
            return new AttendancePersistenceResult(0, 0, skippedRows, duplicateRows);
        }

        dailyAttendanceInternalRepository.saveAll(entitiesToSave);
        updateLogEntries.forEach(entry -> log.info(
                "Attendance update completed. employeeId={}, employeeCode={}, attendanceDate={}, sourceType={}, updatedFields={}, result={}",
                entry.employeeId(),
                entry.employeeCode(),
                entry.attendanceDate(),
                entry.sourceType(),
                entry.updatedFields(),
                entry.result()));
        return new AttendancePersistenceResult(insertedRows, updatedRows, skippedRows, duplicateRows);
    }

    private List<String> applyApiRecord(
            DailyAttendanceInternalEntity entity,
            EmployeeEntity employee,
            InternalAttendanceDayRecord apiRecord) {
        List<String> updatedFields = new ArrayList<>();
        String employeeCode = normalizeText(apiRecord.getUniqueCode());
        String inTime = normalizeText(apiRecord.getInTime());
        String outTime = normalizeText(apiRecord.getOutTime());
        String mappedStatus = mapApiStatus(apiRecord.getStatus());

        entity.setEmployeeId(employee.getEmployeeId());
        if (StringUtils.hasText(employeeCode)) {
            entity.setEmployeeCode(employeeCode);
            updatedFields.add("employee_code");
        } else if (!StringUtils.hasText(entity.getEmployeeCode()) && StringUtils.hasText(employee.getEmployeeCode())) {
            entity.setEmployeeCode(employee.getEmployeeCode().trim());
            updatedFields.add("employee_code");
        }
        entity.setAttendanceDate(apiRecord.getAttendanceDate());
        entity.setApiStatus("Y");
        updatedFields.add("api_status");
        if (StringUtils.hasText(inTime)) {
            entity.setInTime(inTime);
            updatedFields.add("in_time");
        }
        if (StringUtils.hasText(outTime)) {
            entity.setOutTime(outTime);
            updatedFields.add("out_time");
        }
        AttendanceEventWindow eventWindow = AttendanceEventTimeResolver.resolve(entity);
        String totalHours = AttendanceEventTimeResolver.calculateTotalHours(eventWindow);
        if (StringUtils.hasText(totalHours)) {
            entity.setTotalHours(totalHours);
            updatedFields.add("total_hours");
        }
        if (eventWindow.hasAttendanceEvent()) {
            entity.setStatus(PRESENT);
            updatedFields.add("status");
        } else if (StringUtils.hasText(mappedStatus)) {
            entity.setStatus(mappedStatus);
            updatedFields.add("status");
        }
        entity.setMonth(apiRecord.getAttendanceDate().getMonthValue());
        entity.setYear(apiRecord.getAttendanceDate().getYear());
        updatedFields.add("month_val");
        updatedFields.add("year_val");
        return List.copyOf(updatedFields);
    }

    private boolean hasValidApiAttendanceData(InternalAttendanceDayRecord apiRecord) {
        return apiRecord != null
                && apiRecord.getAttendanceDate() != null
                && (StringUtils.hasText(apiRecord.getInTime())
                        || StringUtils.hasText(apiRecord.getOutTime())
                        || StringUtils.hasText(apiRecord.getStatus()));
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

    private InternalAttendanceDayRecord mergeApiRecords(
            InternalAttendanceDayRecord left,
            InternalAttendanceDayRecord right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        AttendanceEventWindow eventWindow = AttendanceEventTimeResolver.resolve(
                AttendanceEventTimeResolver.parse(left.getInTime()),
                AttendanceEventTimeResolver.parse(left.getOutTime()),
                AttendanceEventTimeResolver.parse(right.getInTime()),
                AttendanceEventTimeResolver.parse(right.getOutTime()));
        String status = eventWindow.hasAttendanceEvent()
                ? PRESENT
                : preferText(right.getStatus(), left.getStatus());
        return new InternalAttendanceDayRecord(
                preferText(right.getEmployeeName(), left.getEmployeeName()),
                preferText(right.getUniqueCode(), left.getUniqueCode()),
                left.getAttendanceDate() != null ? left.getAttendanceDate() : right.getAttendanceDate(),
                AttendanceEventTimeResolver.format(eventWindow.inTime()),
                AttendanceEventTimeResolver.format(eventWindow.outTime()),
                status);
    }

    private String preferText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : normalizeText(fallback);
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

    private record AttendanceUpdateLogEntry(
            Long employeeId,
            String employeeCode,
            LocalDate attendanceDate,
            String sourceType,
            List<String> updatedFields,
            String result) {
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
