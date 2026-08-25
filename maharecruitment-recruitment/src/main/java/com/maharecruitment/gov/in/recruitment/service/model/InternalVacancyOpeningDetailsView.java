package com.maharecruitment.gov.in.recruitment.service.model;

import java.time.LocalDateTime;
import java.util.List;

import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyRequirementForm;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyHiringRequestType;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyOpeningDetailsView {

    private Long internalVacancyOpeningId;
    private String requestId;
    private String projectName;
    private InternalVacancyHiringRequestType hiringRequestType;
    private List<String> replacementEmployeeLabels;
    private String eOfficeApprovalFileName;
    private String remarks;
    private List<InternalVacancyRequirementForm> requirements;
    private long totalVacancies;
    private InternalVacancyOpeningStatus status;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;

    public String getStatusLabel() {
        return status == null ? "UNKNOWN" : status.name().replace('_', ' ');
    }

    public String getStatusCssClass() {
        if (status == null) {
            return "status-unknown";
        }
        return switch (status) {
            case DRAFT -> "status-draft";
            case PENDING_HR_APPROVAL -> "status-pending";
            case OPEN -> "status-open";
            case REJECTED_BY_HR -> "status-rejected";
            case CLOSED -> "status-closed";
        };
    }

    public String getHiringRequestTypeLabel() {
        return hiringRequestType == null
                ? "NEW CANDIDATE"
                : hiringRequestType.name().replace('_', ' ');
    }

    public boolean isEmployeeReplacement() {
        return hiringRequestType == InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT;
    }

    public boolean isNewCandidate() {
        return !isEmployeeReplacement();
    }

    public boolean isEditable() {
        return status == InternalVacancyOpeningStatus.DRAFT
                || status == InternalVacancyOpeningStatus.PENDING_HR_APPROVAL;
    }
}
