package com.maharecruitment.gov.in.attendance.entity;

public enum CalendarEntryType {
    HOLIDAY("Holiday"),
    FESTIVAL("Festival"),
    EVENT("Event");

    private final String displayName;

    CalendarEntryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
