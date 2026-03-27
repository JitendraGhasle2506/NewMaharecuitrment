package com.maharecruitment.gov.in.recruitment.service.model;

import org.springframework.util.StringUtils;

public final class InternalVacancyLevelTwoWorkflowStatusResolver {

    private InternalVacancyLevelTwoWorkflowStatusResolver() {
    }

    public static InternalVacancyLevelTwoWorkflowStatus resolveForAgency(
            InternalVacancyLevelTwoWorkflowStatus storedStatus,
            boolean readyForL2,
            boolean scheduled,
            boolean panelAssigned,
            boolean rescheduleRequested,
            int panelFeedbackSubmittedCount,
            String finalDecisionStatus) {
        if (storedStatus != null) {
            return storedStatus;
        }
        InternalVacancyLevelTwoWorkflowStatus finalStatus = resolveFinalDecisionStatus(finalDecisionStatus);
        if (finalStatus != null) {
            return finalStatus;
        }
        if (rescheduleRequested) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_RESCHEDULE_REQUESTED;
        }
        if (panelFeedbackSubmittedCount > 0) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_UNDER_HR_REVIEW;
        }
        if (panelAssigned) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_PANEL_ASSIGNED;
        }
        if (scheduled) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_SCHEDULED;
        }
        if (readyForL2) {
            return InternalVacancyLevelTwoWorkflowStatus.READY_FOR_L2;
        }
        return null;
    }

    public static InternalVacancyLevelTwoWorkflowStatus resolveForHr(
            InternalVacancyLevelTwoWorkflowStatus storedStatus,
            boolean scheduled,
            boolean panelAssigned,
            boolean rescheduleRequested,
            int panelFeedbackSubmittedCount,
            String finalDecisionStatus) {
        return resolveForAgency(
                storedStatus,
                false,
                scheduled,
                panelAssigned,
                rescheduleRequested,
                panelFeedbackSubmittedCount,
                finalDecisionStatus);
    }

    public static InternalVacancyLevelTwoWorkflowStatus resolveForPanel(
            InternalVacancyLevelTwoWorkflowStatus storedStatus,
            boolean scheduled,
            boolean panelAssigned,
            boolean rescheduleRequested,
            boolean myFeedbackSubmitted,
            String finalDecisionStatus) {
        if (storedStatus != null) {
            return storedStatus;
        }
        InternalVacancyLevelTwoWorkflowStatus finalStatus = resolveFinalDecisionStatus(finalDecisionStatus);
        if (finalStatus != null) {
            return finalStatus;
        }
        if (rescheduleRequested) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_RESCHEDULE_REQUESTED;
        }
        if (myFeedbackSubmitted) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_FEEDBACK_SUBMITTED;
        }
        if (panelAssigned) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_PANEL_ASSIGNED;
        }
        if (scheduled) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_SCHEDULED;
        }
        return null;
    }

    private static InternalVacancyLevelTwoWorkflowStatus resolveFinalDecisionStatus(String finalDecisionStatus) {
        if (!StringUtils.hasText(finalDecisionStatus)) {
            return null;
        }
        String normalizedStatus = finalDecisionStatus.trim().toUpperCase();
        if ("SELECTED".equals(normalizedStatus)) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_SELECTED;
        }
        if ("REJECTED".equals(normalizedStatus)) {
            return InternalVacancyLevelTwoWorkflowStatus.L2_REJECTED;
        }
        return null;
    }
}
