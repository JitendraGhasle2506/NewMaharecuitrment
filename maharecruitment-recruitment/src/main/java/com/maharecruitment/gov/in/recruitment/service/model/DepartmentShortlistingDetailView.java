package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentShortlistingDetailView {

    private Long recruitmentNotificationId;
    private String requestId;
    private Long departmentProjectApplicationId;
    private String projectName;
    private List<DepartmentSubmittedCandidateView> candidates;
}
