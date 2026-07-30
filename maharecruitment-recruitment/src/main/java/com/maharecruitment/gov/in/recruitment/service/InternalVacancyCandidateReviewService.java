package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestSummaryView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateRequestListMetricsView;

public interface InternalVacancyCandidateReviewService {

    Page<InternalVacancyCandidateRequestSummaryView> getCandidateRequestSummaryPage(String searchText, Pageable pageable);

    InternalVacancyCandidateRequestListMetricsView getCandidateRequestSummaryMetrics(String searchText);

    InternalVacancyCandidateListView getSubmittedCandidatesByRequestId(String requestId);
}
