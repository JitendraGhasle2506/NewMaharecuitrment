package com.maharecruitment.gov.in.attendance.client;

import java.time.LocalDate;
import java.time.LocalTime;

public interface InternalAttendanceUpdateClient {

    void updateCheckIn(String employeeCode, LocalDate attendanceDate, LocalTime checkInTime);

    void updateCheckOut(String employeeCode, LocalDate attendanceDate, LocalTime checkOutTime);
}
