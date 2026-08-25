package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.ResourceLevelExperience;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationActionService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateSubmissionInput;
import com.maharecruitment.gov.in.master.repository.ResourceLevelExperienceRepository;

class RecruitmentAgencyCandidateServiceImplTest {

    private final AtomicBoolean duplicateSubmittedEmail = new AtomicBoolean(false);
    private final AtomicInteger saveCandidateCallCount = new AtomicInteger(0);
    private final AtomicInteger deletePreOnboardingCallCount = new AtomicInteger(0);
    private final AtomicInteger deleteAssessmentCallCount = new AtomicInteger(0);
    private final AtomicReference<List<RecruitmentInterviewDetailEntity>> savedCandidates = new AtomicReference<>();
    private final AtomicReference<ResourceLevelExperience> configuredLevelExperience = new AtomicReference<>();
    private RecruitmentInterviewDetailEntity withdrawCandidate;
    private AgencyCandidatePreOnboardingEntity withdrawPreOnboarding;
    private RecruitmentAgencyCandidateServiceImpl service;

    @BeforeEach
    void setUp() {
        RecruitmentNotificationEntity notification = new RecruitmentNotificationEntity();
        notification.setRecruitmentNotificationId(11L);
        notification.setRequestId("REQ-11");
        notification.setStatus(RecruitmentNotificationStatus.IN_PROGRESS);

        AgencyMaster agency = new AgencyMaster();
        agency.setAgencyId(22L);

        AgencyNotificationTrackingEntity tracking = new AgencyNotificationTrackingEntity();
        tracking.setAgencyNotificationTrackingId(33L);
        tracking.setRecruitmentNotification(notification);
        tracking.setAgency(agency);
        tracking.setReleasedRank(1);
        tracking.setNotifiedAt(LocalDateTime.now());
        tracking.setStatus(AgencyNotificationTrackingStatus.RELEASED);

        RecruitmentDesignationVacancyEntity vacancy = new RecruitmentDesignationVacancyEntity();
        vacancy.setRecruitmentDesignationVacancyId(44L);
        vacancy.setNotification(notification);
        vacancy.setLevelCode("L1");
        vacancy.setNumberOfVacancy(5L);
        vacancy.setFillPost(0L);

        withdrawCandidate = new RecruitmentInterviewDetailEntity();
        withdrawCandidate.setRecruitmentInterviewDetailId(77L);
        withdrawCandidate.setRecruitmentNotification(notification);
        withdrawCandidate.setAgency(agency);
        withdrawCandidate.setDesignationVacancy(vacancy);
        withdrawCandidate.setCandidateName("Withdraw Candidate");
        withdrawCandidate.setCandidateEmail("withdraw@example.com");
        withdrawCandidate.setCandidateMobile("9876543210");
        withdrawCandidate.setCandidateEducation("B.E.");
        withdrawCandidate.setTotalExperience(new BigDecimal("4"));
        withdrawCandidate.setRelevantExperience(new BigDecimal("3"));
        withdrawCandidate.setJoiningTime("Immediate");
        withdrawCandidate.setResumeOriginalName("resume.pdf");
        withdrawCandidate.setResumeFilePath("resume.pdf");
        withdrawCandidate.setCandidateStatus(RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT);
        withdrawCandidate.setActive(true);

        withdrawPreOnboarding = new AgencyCandidatePreOnboardingEntity();
        withdrawPreOnboarding.setPreOnboardingId(88L);
        withdrawPreOnboarding.setInterviewDetail(withdrawCandidate);

        RecruitmentInterviewDetailRepository interviewDetailRepository = proxyWithDefaults(
                RecruitmentInterviewDetailRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndActiveTrueAndCandidateEmailIgnoreCase" ->
                        duplicateSubmittedEmail.get();
                    case "existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndActiveTrueAndCandidateMobile" ->
                        false;
                    case "saveAll" -> {
                        @SuppressWarnings("unchecked")
                        List<RecruitmentInterviewDetailEntity> candidates =
                                (List<RecruitmentInterviewDetailEntity>) args[0];
                        savedCandidates.set(candidates);
                        yield candidates;
                    }
                    case "findByRecruitmentInterviewDetailIdAndRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId" ->
                        Optional.of(withdrawCandidate);
                    case "save" -> {
                        saveCandidateCallCount.incrementAndGet();
                        yield args[0];
                    }
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentDesignationVacancyRepository designationVacancyRepository = proxyWithDefaults(
                RecruitmentDesignationVacancyRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRecruitmentDesignationVacancyIdAndNotificationRecruitmentNotificationId" -> Optional.of(vacancy);
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentNotificationRepository notificationRepository = proxyWithDefaults(
                RecruitmentNotificationRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIdForUpdate" -> Optional.of(notification);
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        AgencyNotificationTrackingRepository trackingRepository = proxyWithDefaults(
                AgencyNotificationTrackingRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId" -> Optional.of(tracking);
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        AgencyCandidatePreOnboardingRepository preOnboardingRepository = proxyWithDefaults(
                AgencyCandidatePreOnboardingRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByInterviewDetailRecruitmentInterviewDetailId" -> Optional.ofNullable(withdrawPreOnboarding);
                    case "delete" -> {
                        deletePreOnboardingCallCount.incrementAndGet();
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        EmployeeRepository employeeRepository = proxyWithDefaults(
                EmployeeRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase" -> 0L;
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository = proxyWithDefaults(
                RecruitmentAssessmentFeedbackRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRecruitmentInterviewDetailRecruitmentInterviewDetailId" -> Optional.empty();
                    case "delete" -> {
                        deleteAssessmentCallCount.incrementAndGet();
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentAgencyNotificationActionService actionService = proxyWithDefaults(
                RecruitmentAgencyNotificationActionService.class,
                (proxy, method, args) -> null);

        ResourceLevelExperienceRepository levelExperienceRepository = proxyWithDefaults(
                ResourceLevelExperienceRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase" ->
                        Optional.ofNullable(configuredLevelExperience.get());
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        service = new RecruitmentAgencyCandidateServiceImpl(
                interviewDetailRepository,
                designationVacancyRepository,
                notificationRepository,
                trackingRepository,
                preOnboardingRepository,
                employeeRepository,
                assessmentFeedbackRepository,
                actionService,
                levelExperienceRepository);
    }

    @Test
    void submitCandidatesRejectsDuplicateSubmittedEmail() {
        duplicateSubmittedEmail.set(true);

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.submitCandidates(11L, 22L, 55L, 44L, List.of(validInput())));

        assertEquals(
                "Candidate email already exists in submitted candidates for this notification: candidate@example.com",
                exception.getMessage());
    }

    @Test
    void submitCandidatesRejectsMobileThatIsNotExactlyTenDigits() {
        AgencyCandidateSubmissionInput invalidInput = AgencyCandidateSubmissionInput.builder()
                .candidateName("Candidate One")
                .email("candidate@example.com")
                .mobile("987654321")
                .candidateEducation("B.E.")
                .totalExperience(new BigDecimal("4"))
                .relevantExperience(new BigDecimal("3"))
                .currentCtc(new BigDecimal("600000.00"))
                .resigned(false)
                .joiningTime("Immediate")
                .resumeOriginalName("resume.pdf")
                .resumeFilePath("recruitment/resume.pdf")
                .resumeFileType("application/pdf")
                .resumeFileSize(1024L)
                .build();

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.submitCandidates(11L, 22L, 55L, 44L, List.of(invalidInput)));

        assertEquals("Candidate mobile must be 10 digits in row 1.", exception.getMessage());
    }

    @Test
    void submitCandidatesRequiresLastWorkingDayWhenCandidateHasResigned() {
        AgencyCandidateSubmissionInput resignedCandidate = AgencyCandidateSubmissionInput.builder()
                .candidateName("Candidate One")
                .email("candidate@example.com")
                .mobile("9876543210")
                .candidateEducation("B.E.")
                .totalExperience(new BigDecimal("4"))
                .relevantExperience(new BigDecimal("3"))
                .currentCtc(new BigDecimal("600000.00"))
                .resigned(true)
                .joiningTime("Immediate")
                .resumeOriginalName("resume.pdf")
                .resumeFilePath("recruitment/resume.pdf")
                .resumeFileType("application/pdf")
                .resumeFileSize(1024L)
                .build();

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.submitCandidates(11L, 22L, 55L, 44L, List.of(resignedCandidate)));

        assertEquals("Last working day is required for a resigned candidate in row 1.", exception.getMessage());
    }

    @Test
    void submitCandidatesPersistsCtcAndResignationDetails() {
        service.submitCandidates(11L, 22L, 55L, 44L, List.of(validInput()));

        RecruitmentInterviewDetailEntity savedCandidate = savedCandidates.get().getFirst();
        assertEquals(new BigDecimal("600000.00"), savedCandidate.getCurrentCtc());
        assertEquals(true, savedCandidate.getResigned());
        assertEquals(LocalDate.of(2026, 8, 31), savedCandidate.getLastWorkingDay());
    }

    @Test
    void submitCandidatesAllowsExperienceAboveConfiguredMaximum() {
        configuredLevelExperience.set(ResourceLevelExperience.builder()
                .minExperience(new BigDecimal("2"))
                .maxExperience(new BigDecimal("5"))
                .build());

        service.submitCandidates(
                11L,
                22L,
                55L,
                44L,
                List.of(validInput(new BigDecimal("8"), new BigDecimal("6"))));

        assertEquals(new BigDecimal("8"), savedCandidates.get().getFirst().getTotalExperience());
    }

    @Test
    void submitCandidatesRejectsExperienceBelowConfiguredMinimum() {
        configuredLevelExperience.set(ResourceLevelExperience.builder()
                .minExperience(new BigDecimal("2"))
                .maxExperience(new BigDecimal("5"))
                .build());

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.submitCandidates(
                        11L,
                        22L,
                        55L,
                        44L,
                        List.of(validInput(new BigDecimal("1"), new BigDecimal("1")))));

        assertEquals("Total experience must be at least 2 year(s) in row 1.", exception.getMessage());
    }

    @Test
    void withdrawCandidateMarksCandidateInactiveWithoutDeletingRelatedRecords() {
        service.withdrawCandidate(11L, 77L, 22L);

        assertEquals(false, withdrawCandidate.getActive());
        assertEquals(1, saveCandidateCallCount.get());
        assertEquals(0, deletePreOnboardingCallCount.get());
        assertEquals(0, deleteAssessmentCallCount.get());
    }

    private AgencyCandidateSubmissionInput validInput() {
        return validInput(new BigDecimal("4"), new BigDecimal("3"));
    }

    private AgencyCandidateSubmissionInput validInput(BigDecimal totalExperience, BigDecimal relevantExperience) {
        return AgencyCandidateSubmissionInput.builder()
                .candidateName("Candidate One")
                .email("candidate@example.com")
                .mobile("9876543210")
                .candidateEducation("B.E.")
                .totalExperience(totalExperience)
                .relevantExperience(relevantExperience)
                .currentCtc(new BigDecimal("600000.00"))
                .resigned(true)
                .lastWorkingDay(LocalDate.of(2026, 8, 31))
                .joiningTime("Immediate")
                .resumeOriginalName("resume.pdf")
                .resumeFilePath("recruitment/resume.pdf")
                .resumeFileType("application/pdf")
                .resumeFileSize(1024L)
                .build();
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
