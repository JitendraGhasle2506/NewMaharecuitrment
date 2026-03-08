package com.maharecruitment.gov.in.master.entity;

public enum AgencyEntityType {
    PRIVATE_LIMITED("Private Ltd."),
    PUBLIC_LIMITED("Public Ltd."),
    INDIVIDUAL("Individual"),
    PARTNERSHIP_FIRM("Partnership Firm"),
    PROPRIETOR("Proprietor");

    private final String displayName;

    AgencyEntityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
