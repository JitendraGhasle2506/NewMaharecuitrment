package com.maharecruitment.gov.in.attendance.client.model;

import java.time.LocalDate;

public class InternalAttendanceDayRecord {

    private final String employeeName;
    private final String uniqueCode;
    private final LocalDate attendanceDate;
    private final String inTime;
    private final String outTime;
    private final String status;

    public InternalAttendanceDayRecord(
            String employeeName,
            String uniqueCode,
            LocalDate attendanceDate,
            String inTime,
            String outTime,
            String status) {
        this.employeeName = employeeName;
        this.uniqueCode = uniqueCode;
        this.attendanceDate = attendanceDate;
        this.inTime = inTime;
        this.outTime = outTime;
        this.status = status;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public String getInTime() {
        return inTime;
    }

    public String getOutTime() {
        return outTime;
    }

    public String getStatus() {
        return status;
    }
}
