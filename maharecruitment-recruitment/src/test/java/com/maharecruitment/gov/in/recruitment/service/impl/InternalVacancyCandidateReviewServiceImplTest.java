package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyPanelAssessmentRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;

class InternalVacancyCandidateReviewServiceImplTest {

    @Test
    void ownerScopedCandidateListLoadsOnlyAfterOwnershipCheck() {
        RecruitmentNotificationRepository notificationRepository = mock(RecruitmentNotificationRepository.class);
        RecruitmentInterviewDetailRepository interviewDetailRepository = mock(RecruitmentInterviewDetailRepository.class);
        InternalVacancyPanelAssessmentRepository assessmentRepository = mock(InternalVacancyPanelAssessmentRepository.class);
        RecruitmentNotificationEntity notification = mock(RecruitmentNotificationEntity.class);
        InternalVacancyOpeningEntity opening = mock(InternalVacancyOpeningEntity.class);
        RecruitmentInterviewDetailEntity candidate = mock(RecruitmentInterviewDetailEntity.class);
        when(notificationRepository.findInternalVacancyForOwnerByRequestId("REQ-23", "hod@example.com"))
                .thenReturn(Optional.of(notification));
        when(notification.getRecruitmentNotificationId()).thenReturn(31L);
        when(notification.getRequestId()).thenReturn("REQ-23");
        when(notification.getInternalVacancyOpening()).thenReturn(opening);
        when(opening.getInternalVacancyOpeningId()).thenReturn(23L);
        when(candidate.getDepartmentShortlistRemarks()).thenReturn("Strong profile");
        when(interviewDetailRepository.findActiveCandidatesForInternalVacancyByRequestId("REQ-23"))
                .thenReturn(List.of(candidate));
        InternalVacancyCandidateReviewServiceImpl service = new InternalVacancyCandidateReviewServiceImpl(
                notificationRepository,
                interviewDetailRepository,
                assessmentRepository);

        InternalVacancyCandidateListView result = service.getSubmittedCandidatesByRequestIdForOwner(
                " req-23 ",
                " hod@example.com ");

        assertThat(result.getRequestId()).isEqualTo("REQ-23");
        assertThat(result.getInternalVacancyOpeningId()).isEqualTo(23L);
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().getFirst().getDepartmentShortlistRemarks()).isEqualTo("Strong profile");
        verify(notificationRepository).findInternalVacancyForOwnerByRequestId("REQ-23", "hod@example.com");
        verify(interviewDetailRepository).findActiveCandidatesForInternalVacancyByRequestId("REQ-23");
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    void nonOwnerCannotLoadCandidateApplications() {
        RecruitmentNotificationRepository notificationRepository = mock(RecruitmentNotificationRepository.class);
        RecruitmentInterviewDetailRepository interviewDetailRepository = mock(RecruitmentInterviewDetailRepository.class);
        InternalVacancyPanelAssessmentRepository assessmentRepository = mock(InternalVacancyPanelAssessmentRepository.class);
        when(notificationRepository.findInternalVacancyForOwnerByRequestId("REQ-23", "other@example.com"))
                .thenReturn(Optional.empty());
        InternalVacancyCandidateReviewServiceImpl service = new InternalVacancyCandidateReviewServiceImpl(
                notificationRepository,
                interviewDetailRepository,
                assessmentRepository);

        assertThatThrownBy(() -> service.getSubmittedCandidatesByRequestIdForOwner(
                "REQ-23",
                "other@example.com"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessageContaining("not allowed");

        verify(interviewDetailRepository, never()).findActiveCandidatesForInternalVacancyByRequestId("REQ-23");
    }
}
