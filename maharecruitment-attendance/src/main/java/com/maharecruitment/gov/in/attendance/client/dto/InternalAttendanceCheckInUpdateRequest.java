package com.maharecruitment.gov.in.attendance.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InternalAttendanceCheckInUpdateRequest(
        @JsonProperty("employee_code") String employeeCode,
        String date,
        @JsonProperty("in_time") String inTime) {
}
