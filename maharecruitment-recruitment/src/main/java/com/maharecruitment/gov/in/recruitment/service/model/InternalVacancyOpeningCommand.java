package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyOpeningCommand {

    private Long internalVacancyOpeningId;
    private Long projectId;
    private String remarks;
    private String actorEmail;
    private InternalVacancyOpeningStatus targetStatus;
    private List<InternalVacancyRequirementCommand> requirements;
}
