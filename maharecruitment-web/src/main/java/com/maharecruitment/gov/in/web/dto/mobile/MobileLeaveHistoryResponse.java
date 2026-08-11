package com.maharecruitment.gov.in.web.dto.mobile;

import java.util.List;

public record MobileLeaveHistoryResponse(
        boolean success,
        String message,
        Long employeeId,
        List<MobileLeaveApplication> leaveApplications) {

    public MobileLeaveHistoryResponse {
        leaveApplications = leaveApplications == null ? List.of() : List.copyOf(leaveApplications);
    }
}
