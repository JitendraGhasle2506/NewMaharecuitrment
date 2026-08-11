package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MobileLeaveApprovalsResponse(
        boolean success,
        String message,
        Long employeeId,
        String query,
        List<ApprovalItem> pendingLeaves,
        List<ApprovalItem> processedLeaves) {

    public MobileLeaveApprovalsResponse {
        pendingLeaves = pendingLeaves == null ? List.of() : List.copyOf(pendingLeaves);
        processedLeaves = processedLeaves == null ? List.of() : List.copyOf(processedLeaves);
    }

    public record ApprovalItem(
            Long leaveId,
            Long applicantEmployeeId,
            String employeeCode,
            String employeeName,
            String designation,
            String leaveType,
            String leaveCategory,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate compOffWorkDate,
            String description,
            LocalDateTime applicationDate,
            String status,
            String hodRemarks) {
    }
}
