package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
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
        vacancy.setNumberOfVacancy(5L);
        vacancy.setFillPost(0L);

        RecruitmentInterviewDetailRepository interviewDetailRepository = proxyWithDefaults(
                RecruitmentInterviewDetailRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndActiveTrueAndCandidateEmailIgnoreCase" ->
                        duplicateSubmittedEmail.get();
                    case "existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndActiveTrueAndCandidateMobile" ->
                        false;
                    case "saveAll" -> args[0];
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
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        EmployeeRepository employeeRepository = proxyWithDefaults(
                EmployeeRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase" -> 0L;
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentAssessmentFeedbackRepository assessmentFeedbackRepository = proxyWithDefaults(
                RecruitmentAssessmentFeedbackRepository.class,
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        RecruitmentAgencyNotificationActionService actionService = proxyWithDefaults(
                RecruitmentAgencyNotificationActionService.class,
                (proxy, method, args) -> null);

        ResourceLevelExperienceRepository levelExperienceRepository = proxyWithDefaults(
                ResourceLevelExperienceRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase" -> Optional.empty();
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

    private AgencyCandidateSubmissionInput validInput() {
        return AgencyCandidateSubmissionInput.builder()
                .candidateName("Candidate One")
                .email("candidate@example.com")
                .mobile("9876543210")
                .candidateEducation("B.E.")
                .totalExperience(new BigDecimal("4"))
                .relevantExperience(new BigDecimal("3"))
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
