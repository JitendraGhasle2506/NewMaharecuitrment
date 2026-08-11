package com.maharecruitment.gov.in.web.dto.mobile;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MobileLeaveApplication(
        Long leaveId,
        Long employeeId,
        String leaveType,
        String leaveCategory,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate compOffWorkDate,
        String description,
        LocalDateTime applicationDate,
        String status,
        String hodRemarks,
        String managerRemarks,
        boolean cancellable) {
}
