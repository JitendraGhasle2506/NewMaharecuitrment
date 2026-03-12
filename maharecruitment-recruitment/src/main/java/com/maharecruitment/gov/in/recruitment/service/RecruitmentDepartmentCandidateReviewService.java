package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewScheduleAvailableCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentSelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentShortlistingProjectView;

public interface RecruitmentDepartmentCandidateReviewService {

    List<DepartmentShortlistingProjectView> getDepartmentShortlistingProjects(Long departmentRegistrationId);

    DepartmentShortlistingDetailView getDepartmentShortlistingDetail(
            Long departmentRegistrationId,
            Long recruitmentNotificationId);

    List<DepartmentSelectedCandidateView> getSelectedCandidates(
            Long departmentRegistrationId,
            Long recruitmentNotificationId);

    List<DepartmentInterviewScheduleAvailableCandidateView> getCandidatesAvailableForInterviewSchedule(
            Long departmentRegistrationId,
            Long recruitmentNotificationId);

    void applyCandidateReviewDecision(
            Long departmentRegistrationId,
            Long departmentUserId,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks);
}
