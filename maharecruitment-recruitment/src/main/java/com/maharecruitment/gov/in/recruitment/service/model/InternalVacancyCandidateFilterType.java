package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum InternalVacancyCandidateFilterType {
    ALL("all", "All Candidates"),
    PENDING_REVIEW("pending", "Pending Review"),
    SHORTLISTED("shortlisted", "Shortlisted"),
    REJECTED("rejected", "Rejected"),
    INTERVIEW_SCHEDULED("scheduled", "Interview Scheduled"),
    FEEDBACK_SUBMITTED("feedback", "Feedback Submitted");

    private final String requestValue;
    private final String label;

    InternalVacancyCandidateFilterType(String requestValue, String label) {
        this.requestValue = requestValue;
        this.label = label;
    }

    public static InternalVacancyCandidateFilterType fromRequestValue(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return Arrays.stream(values())
                .filter(filterType -> filterType.requestValue.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(ALL);
    }
}
