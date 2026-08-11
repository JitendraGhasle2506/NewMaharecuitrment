package com.maharecruitment.gov.in.web.event.mobile;

import java.time.LocalDate;
import java.time.LocalTime;

public record MobileCheckOutRecordedEvent(
        String employeeCode,
        LocalDate attendanceDate,
        LocalTime checkOutTime) {
}
