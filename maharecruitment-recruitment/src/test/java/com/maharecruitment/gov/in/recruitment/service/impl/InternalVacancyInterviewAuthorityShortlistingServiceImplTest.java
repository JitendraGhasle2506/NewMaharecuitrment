package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentCandidateReviewDecision;

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

        service = new InternalVacancyInterviewAuthorityShortlistingServiceImpl(
                userRepository,
                employeeRepository,
                notificationRepository,
                interviewDetailRepository);
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

        assertTrue(template.contains("candidateListView.canShortlistCandidates == true"));
        assertTrue(template.contains("Only the user who raised this request can shortlist or reject candidates."));
        assertTrue(template.contains("candidateListView.canSubmitInterviewFeedback == true"));
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
