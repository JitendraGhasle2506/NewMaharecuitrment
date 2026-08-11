package com.maharecruitment.gov.in.web.event.mobile;

import java.time.LocalDate;
import java.time.LocalTime;

public record MobileCheckInRecordedEvent(
        String employeeCode,
        LocalDate attendanceDate,
        LocalTime checkInTime) {
}
