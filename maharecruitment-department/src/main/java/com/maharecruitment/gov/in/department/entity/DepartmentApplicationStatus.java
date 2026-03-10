package com.maharecruitment.gov.in.department.entity;

import java.util.Arrays;

public enum DepartmentApplicationStatus {
    DRAFT(1),
    SUBMITTED_TO_HR(2),
    HR_SENT_BACK(3),
    CORRECTED_BY_DEPARTMENT(4),
    HR_APPROVED(5),
    HR_REJECTED(6),
    AUDITOR_REVIEW(7),
    AUDITOR_SENT_BACK(8),
    AUDITOR_APPROVED(9),
    COMPLETED(10);

    private final int statusCode;

    DepartmentApplicationStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static DepartmentApplicationStatus fromStatusCode(int statusCode) {
        return Arrays.stream(values())
                .filter(status -> status.statusCode == statusCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported status code: " + statusCode));
    }
}
