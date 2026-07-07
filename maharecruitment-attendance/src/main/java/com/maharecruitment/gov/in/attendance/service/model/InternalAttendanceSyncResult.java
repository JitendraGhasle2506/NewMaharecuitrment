package com.maharecruitment.gov.in.attendance.service.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class InternalAttendanceSyncResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean enabled;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int employeesAttempted;
    private final int employeesSynced;
    private final int employeesSkipped;
    private final int employeesFailed;
    private final int attendanceRowsUpserted;
    private final int attendanceRowsInserted;
    private final int attendanceRowsUpdated;
    private final int attendanceRowsSkipped;
    private final int duplicateRows;
    private final long apiTimeMillis;
    private final long totalProcessingTimeMillis;
    private final String failureMessage;

    public InternalAttendanceSyncResult(
            boolean enabled,
            LocalDate startDate,
            LocalDate endDate,
            int employeesAttempted,
            int employeesSynced,
            int employeesSkipped,
            int employeesFailed,
            int attendanceRowsUpserted) {
        this(enabled, startDate, endDate, employeesAttempted, employeesSynced, employeesSkipped, employeesFailed,
                attendanceRowsUpserted, 0, 0, 0, 0, 0L, 0L, null);
    }

    public InternalAttendanceSyncResult(
            boolean enabled,
            LocalDate startDate,
            LocalDate endDate,
            int employeesAttempted,
            int employeesSynced,
            int employeesSkipped,
            int employeesFailed,
            int attendanceRowsUpserted,
            String failureMessage) {
        this(enabled, startDate, endDate, employeesAttempted, employeesSynced, employeesSkipped, employeesFailed,
                attendanceRowsUpserted, 0, 0, 0, 0, 0L, 0L, failureMessage);
    }

    public InternalAttendanceSyncResult(
            boolean enabled,
            LocalDate startDate,
            LocalDate endDate,
            int employeesAttempted,
            int employeesSynced,
            int employeesSkipped,
            int employeesFailed,
            int attendanceRowsUpserted,
            int attendanceRowsInserted,
            int attendanceRowsUpdated,
            int attendanceRowsSkipped,
            int duplicateRows,
            long apiTimeMillis,
            long totalProcessingTimeMillis,
            String failureMessage) {
        this.enabled = enabled;
        this.startDate = startDate;
        this.endDate = endDate;
        this.employeesAttempted = employeesAttempted;
        this.employeesSynced = employeesSynced;
        this.employeesSkipped = employeesSkipped;
        this.employeesFailed = employeesFailed;
        this.attendanceRowsUpserted = attendanceRowsUpserted;
        this.attendanceRowsInserted = attendanceRowsInserted;
        this.attendanceRowsUpdated = attendanceRowsUpdated;
        this.attendanceRowsSkipped = attendanceRowsSkipped;
        this.duplicateRows = duplicateRows;
        this.apiTimeMillis = apiTimeMillis;
        this.totalProcessingTimeMillis = totalProcessingTimeMillis;
        this.failureMessage = failureMessage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getEmployeesAttempted() {
        return employeesAttempted;
    }

    public int getEmployeesSynced() {
        return employeesSynced;
    }

    public int getEmployeesSkipped() {
        return employeesSkipped;
    }

    public int getEmployeesFailed() {
        return employeesFailed;
    }

    public int getAttendanceRowsUpserted() {
        return attendanceRowsUpserted;
    }

    public int getAttendanceRowsInserted() {
        return attendanceRowsInserted;
    }

    public int getAttendanceRowsUpdated() {
        return attendanceRowsUpdated;
    }

    public int getAttendanceRowsSkipped() {
        return attendanceRowsSkipped;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public long getApiTimeMillis() {
        return apiTimeMillis;
    }

    public long getTotalProcessingTimeMillis() {
        return totalProcessingTimeMillis;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
