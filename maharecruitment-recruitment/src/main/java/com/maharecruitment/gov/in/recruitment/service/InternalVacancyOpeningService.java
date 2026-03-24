package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.master.dto.ManpowerDesignationMasterResponse;
import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyOpeningForm;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyInterviewAuthorityRoleOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyInterviewAuthorityUserOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalProjectOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningLevelOptionView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningResult;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningSummaryView;

public interface InternalVacancyOpeningService {

    InternalVacancyOpeningResult saveOpening(InternalVacancyOpeningCommand command);

    List<InternalVacancyOpeningSummaryView> getAllOpenings();

    InternalVacancyOpeningForm getOpeningForEdit(Long internalVacancyOpeningId);

    List<InternalProjectOptionView> getAvailableInternalProjects();

    List<ManpowerDesignationMasterResponse> getAvailableDesignations();

    List<InternalVacancyOpeningLevelOptionView> getLevelsByDesignation(Long designationId);

    List<InternalVacancyInterviewAuthorityRoleOptionView> getAvailableInterviewAuthorityRoles();

    List<InternalVacancyInterviewAuthorityUserOptionView> getAvailableInterviewAuthorities(List<Long> roleIds);
}
