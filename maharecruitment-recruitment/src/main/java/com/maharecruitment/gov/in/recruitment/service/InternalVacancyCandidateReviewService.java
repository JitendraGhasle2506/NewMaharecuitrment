package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;

public interface InternalVacancyCandidateReviewService {

    List<InternalVacancyCandidateRequestSummaryView> getCandidateRequestSummaries();

    InternalVacancyCandidateListView getSubmittedCandidatesByRequestId(String requestId);
}
