package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyCandidateListView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private RecruitmentNotificationStatus notificationStatus;
    private List<InternalVacancySubmittedCandidateView> candidates;
}
