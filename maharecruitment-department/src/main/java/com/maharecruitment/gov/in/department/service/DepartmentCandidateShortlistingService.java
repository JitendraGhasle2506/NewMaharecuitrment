package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingProjectView;

public interface DepartmentCandidateShortlistingService {

    List<DepartmentShortlistingProjectView> getProjectQueue(String actorEmail);

    DepartmentShortlistingDetailView getShortlistingDetail(Long recruitmentNotificationId, String actorEmail);

    void reviewCandidate(
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks,
            String actorEmail);
}
