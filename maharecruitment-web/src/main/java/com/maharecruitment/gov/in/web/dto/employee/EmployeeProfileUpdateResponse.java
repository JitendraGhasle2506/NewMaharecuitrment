package com.maharecruitment.gov.in.web.dto.employee;

import java.util.Map;

public record EmployeeProfileUpdateResponse(
        boolean success,
        String message,
        EmployeeProfileDTO profile,
        Map<String, String> errors) {
}
