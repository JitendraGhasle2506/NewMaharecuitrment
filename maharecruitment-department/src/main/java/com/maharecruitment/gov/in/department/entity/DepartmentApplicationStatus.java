package com.maharecruitment.gov.in.department.entity;

import java.util.Arrays;

public enum DepartmentApplicationStatus {
    DRAFT(1, "Draft"),
    SUBMITTED_TO_HR(2, "Save and Forward to HR"),
    HR_SENT_BACK(3, "Sent Back by HR"),
    CORRECTED_BY_DEPARTMENT(4, "Corrected by Department"),
    HR_APPROVED(5, "Review then Forward to Audit"),
    HR_REJECTED(6, "Rejected by HR"),
    AUDITOR_REVIEW(7, "Auditor Review"),
    AUDITOR_SENT_BACK(8, "Sent Back by Auditor"),
    AUDITOR_APPROVED(9, "Review and Approved"),
    COMPLETED(10, "Completed");

    private final int statusCode;
    private final String displayName;

    DepartmentApplicationStatus(int statusCode, String displayName) {
        this.statusCode = statusCode;
        this.displayName = displayName;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DepartmentApplicationStatus fromStatusCode(int statusCode) {
        return Arrays.stream(values())
                .filter(status -> status.statusCode == statusCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported status code: " + statusCode));
    }
}
