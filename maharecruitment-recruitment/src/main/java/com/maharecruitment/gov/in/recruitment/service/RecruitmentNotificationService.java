package com.maharecruitment.gov.in.recruitment.service;

import com.maharecruitment.gov.in.recruitment.service.model.AuditorApprovedNotificationCommand;

public interface RecruitmentNotificationService {

    void upsertFromAuditorApproval(AuditorApprovedNotificationCommand command);

    void upsertFromInternalVacancyOpening(Long internalVacancyOpeningId);

    void closeFromInternalVacancyOpening(Long internalVacancyOpeningId);
}
