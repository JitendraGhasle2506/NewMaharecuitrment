package com.maharecruitment.gov.in.master.entity;

public enum AgencyBankAccountType {
    SAVINGS("Savings"),
    CURRENT("Current");

    private final String displayName;

    AgencyBankAccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
