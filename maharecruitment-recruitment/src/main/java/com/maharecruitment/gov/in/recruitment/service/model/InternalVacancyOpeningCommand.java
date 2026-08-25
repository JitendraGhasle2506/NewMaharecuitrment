package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyHiringRequestType;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyOpeningCommand {

    private Long internalVacancyOpeningId;
    private Long projectId;
    private InternalVacancyHiringRequestType hiringRequestType;
    private List<Long> replacementEmployeeIds;
    private MultipartFile eOfficeApprovalDocument;
    private String remarks;
    private String actorEmail;
    private InternalVacancyOpeningStatus targetStatus;
    private List<InternalVacancyRequirementCommand> requirements;
    private List<Long> interviewAuthorityRoleIds;
    private List<Long> interviewAuthorityUserIds;
    private List<Long> interviewAuthorityEmployeeIds;
}
