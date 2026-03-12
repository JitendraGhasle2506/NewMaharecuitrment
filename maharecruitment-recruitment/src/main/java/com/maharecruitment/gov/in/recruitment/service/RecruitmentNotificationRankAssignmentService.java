package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyRankAssignmentCommand;

public interface RecruitmentNotificationRankAssignmentService {

    void assignAgencyRanks(Long recruitmentNotificationId, List<AgencyRankAssignmentCommand> assignments);
}

