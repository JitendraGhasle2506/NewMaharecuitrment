package com.maharecruitment.gov.in.invoice.entity;

public enum AgencyMonthlyBillEmployeeType {

    ALL("All Employees"),
    EXTERNAL("External Employees"),
    INTERNAL("Internal Employees");

    private final String displayName;

    AgencyMonthlyBillEmployeeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean includesExternal() {
        return this == ALL || this == EXTERNAL;
    }

    public boolean includesInternal() {
        return this == ALL || this == INTERNAL;
    }
}
