package com.maharecruitment.gov.in.master.entity;

public enum DesignationType {
    O("Other"),
    M("MAHAIT On Roll");

    private final String displayName;

    DesignationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
