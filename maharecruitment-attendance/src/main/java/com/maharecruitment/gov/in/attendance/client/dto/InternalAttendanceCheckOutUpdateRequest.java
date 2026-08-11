package com.maharecruitment.gov.in.attendance.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InternalAttendanceCheckOutUpdateRequest(
        @JsonProperty("employee_code") String employeeCode,
        String date,
        @JsonProperty("out_time") String outTime) {
}
