package com.maharecruitment.gov.in.web.service.agency;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySubmittedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyCandidateBatchForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;

public interface AgencyRecruitmentNotificationPageService {

    Page<AgencyVisibleNotificationView> getVisibleNotifications(String actorEmail, String searchText, Pageable pageable);

    AgencyVisibleNotificationListMetricsView getVisibleNotificationMetrics(String actorEmail, String searchText);

    AgencyNotificationDetailView getNotificationDetail(String actorEmail, Long recruitmentNotificationId);

    void markAsRead(String actorEmail, Long recruitmentNotificationId);

    void submitResponse(String actorEmail, Long recruitmentNotificationId);

    List<AgencySubmittedCandidateView> getSubmittedCandidates(String actorEmail, Long recruitmentNotificationId);

    List<AgencySelectedCandidateProjectView> getSelectedCandidateProjects(String actorEmail);

    List<AgencySelectedCandidateView> getSelectedCandidates(String actorEmail, Long recruitmentNotificationId);

    void submitCandidates(String actorEmail, Long recruitmentNotificationId, AgencyCandidateBatchForm candidateBatchForm);

    void scheduleInterview(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            AgencyInterviewScheduleForm interviewScheduleForm);

    void withdrawCandidate(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId);
}
