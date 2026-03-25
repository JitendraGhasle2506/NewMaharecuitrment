package com.maharecruitment.gov.in.recruitment.service.model;

public enum InternalVacancyLevelTwoWorkflowStatus {
    READY_FOR_L2("warning", "text-bg-warning text-dark"),
    L2_SCHEDULED("info", "text-bg-info"),
    L2_PANEL_ASSIGNED("primary", "text-bg-primary"),
    L2_RESCHEDULE_REQUESTED("warning", "text-bg-warning text-dark"),
    L2_FEEDBACK_SUBMITTED("success", "text-bg-success"),
    L2_UNDER_HR_REVIEW("info", "text-bg-info"),
    L2_SELECTED("success", "text-bg-success"),
    L2_REJECTED("danger", "text-bg-danger");

    private final String tone;
    private final String bootstrapBadgeClass;

    InternalVacancyLevelTwoWorkflowStatus(String tone, String bootstrapBadgeClass) {
        this.tone = tone;
        this.bootstrapBadgeClass = bootstrapBadgeClass;
    }

    public String getTone() {
        return tone;
    }

    public String getBootstrapBadgeClass() {
        return bootstrapBadgeClass;
    }
}
