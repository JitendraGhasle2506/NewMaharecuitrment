package com.maharecruitment.gov.in.attendance.service.model;

import java.time.LocalDate;

public class InternalAttendanceSyncResult {

    private final boolean enabled;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int employeesAttempted;
    private final int employeesSynced;
    private final int employeesSkipped;
    private final int employeesFailed;
    private final int attendanceRowsUpserted;

    public InternalAttendanceSyncResult(
            boolean enabled,
            LocalDate startDate,
            LocalDate endDate,
            int employeesAttempted,
            int employeesSynced,
            int employeesSkipped,
            int employeesFailed,
            int attendanceRowsUpserted) {
        this.enabled = enabled;
        this.startDate = startDate;
        this.endDate = endDate;
        this.employeesAttempted = employeesAttempted;
        this.employeesSynced = employeesSynced;
        this.employeesSkipped = employeesSkipped;
        this.employeesFailed = employeesFailed;
        this.attendanceRowsUpserted = attendanceRowsUpserted;
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
}
