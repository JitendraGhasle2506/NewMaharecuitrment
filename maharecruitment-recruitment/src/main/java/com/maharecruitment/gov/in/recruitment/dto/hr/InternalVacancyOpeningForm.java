package com.maharecruitment.gov.in.recruitment.dto.hr;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyHiringRequestType;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalVacancyOpeningForm {

    private Long internalVacancyOpeningId;

    private InternalVacancyOpeningStatus currentStatus = InternalVacancyOpeningStatus.DRAFT;

    @NotNull(message = "Project is required.")
    private Long projectId;

    @NotNull(message = "Hiring request type is required.")
    private InternalVacancyHiringRequestType hiringRequestType = InternalVacancyHiringRequestType.NEW_CANDIDATE;

    private List<Long> replacementEmployeeIds = new ArrayList<>();

    private MultipartFile eOfficeApprovalDocument;

    private String existingEOfficeApprovalFileName;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters.")
    private String remarks;

    @Valid
    private List<InternalVacancyRequirementForm> requirements = new ArrayList<>();

    private List<Long> interviewAuthorityRoleIds = new ArrayList<>();

    private List<Long> interviewAuthorityUserIds = new ArrayList<>();

    private List<Long> interviewAuthorityEmployeeIds = new ArrayList<>();
}
