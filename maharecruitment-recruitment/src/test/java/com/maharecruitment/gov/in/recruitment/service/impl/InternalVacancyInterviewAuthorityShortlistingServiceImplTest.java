package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyPanelAssessmentRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateFilterType;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyCandidateListView;

class InternalVacancyInterviewAuthorityShortlistingServiceImplTest {

    private static final String REQUEST_OWNER_EMAIL = "owner@example.com";

    private final AtomicReference<String> queriedOwnerEmail = new AtomicReference<>();
    private RecruitmentInterviewDetailEntity candidate;
    private InternalVacancyInterviewAuthorityShortlistingServiceImpl service;

    @BeforeEach
    void setUp() {
        User actor = new User();
        actor.setId(15L);
        actor.setEmail(REQUEST_OWNER_EMAIL);

        UserRepository userRepository = proxyWithDefaults(
                UserRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmailIgnoreCase" -> Optional.of(actor);
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        EmployeeRepository employeeRepository = proxyWithDefaults(
                EmployeeRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByUser_Id" -> Optional.empty();
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentNotificationRepository notificationRepository = proxyWithDefaults(
                RecruitmentNotificationRepository.class,
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        candidate = new RecruitmentInterviewDetailEntity();
        candidate.setRecruitmentInterviewDetailId(77L);
        candidate.setCandidateStatus(RecruitmentCandidateStatus.SUBMITTED_BY_AGENCY);

        RecruitmentInterviewDetailRepository interviewDetailRepository = proxyWithDefaults(
                RecruitmentInterviewDetailRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIdForInternalVacancyRequesterReviewUpdate" -> {
                        String actorEmail = (String) args[2];
                        queriedOwnerEmail.set(actorEmail);
                        yield REQUEST_OWNER_EMAIL.equalsIgnoreCase(actorEmail)
                                ? Optional.of(candidate)
                                : Optional.empty();
                    }
                    case "save" -> args[0];
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        InternalVacancyPanelAssessmentRepository assessmentRepository = proxyWithDefaults(
                InternalVacancyPanelAssessmentRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findSubmittedInterviewDetailIdsByAssessor" -> Set.of();
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        service = new InternalVacancyInterviewAuthorityShortlistingServiceImpl(
                userRepository,
                employeeRepository,
                notificationRepository,
                interviewDetailRepository,
                assessmentRepository);
    }

    @Test
    void requestOwnerCanShortlistCandidate() {
        service.reviewCandidate(
                "  OWNER@example.com  ",
                " req-20260825-i0001 ",
                77L,
                DepartmentCandidateReviewDecision.SHORTLIST,
                "Suitable candidate");

        assertEquals(REQUEST_OWNER_EMAIL, queriedOwnerEmail.get());
        assertEquals(RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT, candidate.getCandidateStatus());
        assertEquals(15L, candidate.getDepartmentShortlistedByUserId());
        assertEquals("Suitable candidate", candidate.getDepartmentShortlistRemarks());
    }

    @Test
    void requestOwnerCanRejectCandidateWithRemarks() {
        service.reviewCandidate(
                REQUEST_OWNER_EMAIL,
                "REQ-20260825-I0001",
                77L,
                DepartmentCandidateReviewDecision.REJECT,
                "  Experience does not match  ");

        assertEquals(RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT, candidate.getCandidateStatus());
        assertEquals("Experience does not match", candidate.getDepartmentShortlistRemarks());
    }

    @Test
    void rejectionRequiresRemarks() {
        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.reviewCandidate(
                        REQUEST_OWNER_EMAIL,
                        "REQ-20260825-I0001",
                        77L,
                        DepartmentCandidateReviewDecision.REJECT,
                        "  "));

        assertEquals("Remarks are required when rejecting a candidate.", exception.getMessage());
        assertEquals(RecruitmentCandidateStatus.SUBMITTED_BY_AGENCY, candidate.getCandidateStatus());
    }

    @Test
    void nonOwnerCannotShortlistCandidate() {
        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.reviewCandidate(
                        "assigned.authority@example.com",
                        "REQ-20260825-I0001",
                        77L,
                        DepartmentCandidateReviewDecision.SHORTLIST,
                        null));

        assertEquals(
                "Only the user who raised this internal vacancy request can shortlist or reject candidates.",
                exception.getMessage());
        assertEquals(RecruitmentCandidateStatus.SUBMITTED_BY_AGENCY, candidate.getCandidateStatus());
    }

    @Test
    void candidatePageShowsShortlistActionsOnlyToRequestOwner() throws Exception {
        String template = new ClassPathResource(
                "templates/interview-authority/internal-vacancy-candidate-list.html")
                .getContentAsString(StandardCharsets.UTF_8);
        String feedbackTemplate = new ClassPathResource(
                "templates/interview-authority/internal-vacancy-feedback-form.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(template.contains("candidateListView.canShortlistCandidates == true"));
        assertTrue(template.contains("Only the user who raised this request can shortlist or reject candidates."));
        assertTrue(template.contains("candidateListView.canSubmitInterviewFeedback == true"));
        assertTrue(template.contains("candidate.feedbackSubmittedByCurrentActor"));
        assertTrue(template.contains("Fill Interview Feedback"));
        assertTrue(feedbackTemplate.contains("workflowDetail.assessment == null"));
        assertTrue(feedbackTemplate.contains("workflowDetail.assessment != null ? 'Update Feedback' : 'Submit Feedback'"));
    }

    @Test
    void anotherInterviewersFeedbackDoesNotHideCurrentInterviewersPendingForm() {
        User actor = new User();
        actor.setId(15L);
        actor.setEmail(REQUEST_OWNER_EMAIL);

        UserRepository userRepository = mock(UserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        RecruitmentNotificationRepository notificationRepository = mock(RecruitmentNotificationRepository.class);
        RecruitmentInterviewDetailRepository interviewDetailRepository = mock(RecruitmentInterviewDetailRepository.class);
        InternalVacancyPanelAssessmentRepository assessmentRepository =
                mock(InternalVacancyPanelAssessmentRepository.class);

        InternalVacancyOpeningEntity opening = new InternalVacancyOpeningEntity();
        opening.setInternalVacancyOpeningId(9L);
        opening.setStatus(InternalVacancyOpeningStatus.OPEN);
        opening.setCreatedByEmail("requester@example.com");

        RecruitmentNotificationEntity notification = new RecruitmentNotificationEntity();
        notification.setRecruitmentNotificationId(21L);
        notification.setRequestId("REQ-20260825-I0001");
        notification.setInternalVacancyOpening(opening);

        RecruitmentInterviewDetailEntity candidate = new RecruitmentInterviewDetailEntity();
        candidate.setRecruitmentInterviewDetailId(77L);
        candidate.setRecruitmentNotification(notification);
        candidate.setCandidateStatus(RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY);
        candidate.setAssessmentSubmitted(true);

        when(userRepository.findByEmailIgnoreCase(REQUEST_OWNER_EMAIL)).thenReturn(Optional.of(actor));
        when(employeeRepository.findByUser_Id(15L)).thenReturn(Optional.empty());
        when(notificationRepository.findAccessibleInternalVacancyForCandidateReview(
                "REQ-20260825-I0001",
                REQUEST_OWNER_EMAIL,
                15L,
                null)).thenReturn(Optional.of(notification));
        when(interviewDetailRepository.findActiveCandidatesForInternalVacancyAccessibleToActor(
                "REQ-20260825-I0001",
                REQUEST_OWNER_EMAIL,
                15L,
                null)).thenReturn(List.of(candidate));
        when(assessmentRepository.findSubmittedInterviewDetailIdsByAssessor(Set.of(77L), 15L, null))
                .thenReturn(Set.of());

        InternalVacancyInterviewAuthorityShortlistingServiceImpl candidateService =
                new InternalVacancyInterviewAuthorityShortlistingServiceImpl(
                        userRepository,
                        employeeRepository,
                        notificationRepository,
                        interviewDetailRepository,
                        assessmentRepository);

        InternalVacancyCandidateListView result = candidateService.getAssignedCandidatesByRequestId(
                REQUEST_OWNER_EMAIL,
                "REQ-20260825-I0001",
                InternalVacancyCandidateFilterType.ALL);

        assertFalse(result.getCandidates().getFirst().isFeedbackSubmittedByCurrentActor());
        assertEquals(1L, result.getInterviewScheduledCandidates());
        assertEquals(0L, result.getFeedbackSubmittedCandidates());

        InternalVacancyCandidateListView awaitingMyFeedback = candidateService.getAssignedCandidatesByRequestId(
                REQUEST_OWNER_EMAIL,
                "REQ-20260825-I0001",
                InternalVacancyCandidateFilterType.INTERVIEW_SCHEDULED);
        InternalVacancyCandidateListView mySubmittedFeedback = candidateService.getAssignedCandidatesByRequestId(
                REQUEST_OWNER_EMAIL,
                "REQ-20260825-I0001",
                InternalVacancyCandidateFilterType.FEEDBACK_SUBMITTED);

        assertEquals(1, awaitingMyFeedback.getCandidates().size());
        assertTrue(mySubmittedFeedback.getCandidates().isEmpty());

        when(assessmentRepository.findSubmittedInterviewDetailIdsByAssessor(Set.of(77L), 15L, null))
                .thenReturn(Set.of(77L));
        InternalVacancyCandidateListView submittedByCurrentInterviewer =
                candidateService.getAssignedCandidatesByRequestId(
                        REQUEST_OWNER_EMAIL,
                        "REQ-20260825-I0001",
                        InternalVacancyCandidateFilterType.FEEDBACK_SUBMITTED);

        assertTrue(submittedByCurrentInterviewer.getCandidates().getFirst()
                .isFeedbackSubmittedByCurrentActor());
        assertEquals(0L, submittedByCurrentInterviewer.getInterviewScheduledCandidates());
        assertEquals(1L, submittedByCurrentInterviewer.getFeedbackSubmittedCandidates());
    }

    private static <T> T proxyWithDefaults(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Stub";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(proxy, method, args);
                }));
    }
}
