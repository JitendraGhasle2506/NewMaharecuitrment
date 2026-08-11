package com.maharecruitment.gov.in.web.dto.mobile;

public record MobileLeaveApplicationResponse(
        boolean success,
        String message,
        MobileLeaveApplication leaveApplication) {
}
