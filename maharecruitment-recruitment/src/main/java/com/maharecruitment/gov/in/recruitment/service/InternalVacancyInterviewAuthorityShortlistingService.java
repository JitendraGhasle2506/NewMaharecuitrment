package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateFilterType;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;

public interface InternalVacancyInterviewAuthorityShortlistingService {

    List<InternalVacancyCandidateRequestSummaryView> getAssignedRequestSummaries(String actorEmail);

    InternalVacancyCandidateListView getAssignedCandidatesByRequestId(
            String actorEmail,
            String requestId,
            InternalVacancyCandidateFilterType filterType);

    void reviewCandidate(
            String actorEmail,
            String requestId,
            Long recruitmentInterviewDetailId,
            DepartmentCandidateReviewDecision reviewDecision,
            String reviewRemarks);
}
