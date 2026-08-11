package com.maharecruitment.gov.in.web.dto.mobile;

import java.util.List;

public record MobileLeaveOptionsResponse(
        boolean success,
        String message,
        Long employeeId,
        List<LeaveType> leaveTypes,
        List<LeaveCategory> leaveCategories) {

    public MobileLeaveOptionsResponse {
        leaveTypes = leaveTypes == null ? List.of() : List.copyOf(leaveTypes);
        leaveCategories = leaveCategories == null ? List.of() : List.copyOf(leaveCategories);
    }

    public record LeaveType(
            Long leaveId,
            String code,
            String name,
            boolean compOff) {
    }

    public record LeaveCategory(
            String code,
            String name) {
    }
}
