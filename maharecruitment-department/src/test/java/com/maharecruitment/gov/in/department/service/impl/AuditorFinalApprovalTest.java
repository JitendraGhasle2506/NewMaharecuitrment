package com.maharecruitment.gov.in.department.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.common.event.invoice.DepartmentTaxInvoiceGenerationRequestedEvent;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectResourceRequirementEntity;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProformaInvoiceRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;
import com.maharecruitment.gov.in.master.service.ProjectMstService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationService;

@ExtendWith(MockitoExtension.class)
class AuditorFinalApprovalTest {
    private static final String EMAIL = "auditor@example.test";

    @Mock private DepartmentProjectApplicationRepository applicationRepository;
    @Mock private DepartmentProjectApplicationActivityRepository activityRepository;
    @Mock private DepartmentProformaInvoiceRepository proformaInvoiceRepository;
    @Mock private DepartmentTaxRateMasterRepository taxRateMasterRepository;
    @Mock private UserAffiliationService userAffiliationService;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMstService projectMstService;
    @Mock private RecruitmentNotificationService recruitmentNotificationService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private EntityManager entityManager;
    @InjectMocks private DepartmentManpowerApplicationServiceImpl service;

    private DepartmentProjectApplicationEntity application;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        Role role = new Role();
        role.setName("ROLE_AUDITOR");
        User auditor = new User();
        auditor.setId(1L);
        auditor.setEmail(EMAIL);
        auditor.setName("Auditor");
        auditor.setRoles(List.of(role));
        when(userAffiliationService.loadUserByEmail(EMAIL)).thenReturn(auditor);

        application = new DepartmentProjectApplicationEntity();
        application.setDepartmentProjectApplicationId(37L);
        application.setDepartmentId(3L);
        application.setSubDepartmentId(7L);
        application.setProjectName("Test project");
        application.setApplicationType(DepartmentApplicationType.values()[0]);
        application.setApplicationStatus(DepartmentApplicationStatus.HR_APPROVED);
        DepartmentProjectResourceRequirementEntity requirement = new DepartmentProjectResourceRequirementEntity();
        requirement.setDesignationId(1L);
        requirement.setRequiredQuantity(1);
        requirement.setTaxableAmount(BigDecimal.valueOf(100));
        application.getResourceRequirements().add(requirement);
        when(applicationRepository.findById(37L)).thenReturn(Optional.of(application));
    }

    @ParameterizedTest
    @EnumSource(value = DepartmentApplicationStatus.class, names = {"HR_APPROVED", "AUDITOR_REVIEW"})
    void firstApprovalCompletesApplicationAndGeneratesInvoiceOnce(DepartmentApplicationStatus status) {
        application.setApplicationStatus(status);
        when(applicationRepository.save(application)).thenReturn(application);

        assertThat(service.reviewByAuditor(37L, AuditorReviewDecision.APPROVE, "Verified", EMAIL))
                .isEqualTo(DepartmentApplicationStatus.COMPLETED);
        assertThat(application.getApplicationStatus()).isEqualTo(DepartmentApplicationStatus.COMPLETED);
        verify(entityManager).refresh(application, LockModeType.PESSIMISTIC_WRITE);
        verify(proformaInvoiceRepository).save(any());
        verify(applicationEventPublisher).publishEvent(any(DepartmentTaxInvoiceGenerationRequestedEvent.class));

        assertThatThrownBy(() -> service.reviewByAuditor(37L, AuditorReviewDecision.APPROVE, "Again", EMAIL))
                .isInstanceOf(DepartmentApplicationException.class).hasMessageContaining("final");
        assertThatThrownBy(() -> service.markCompleted(37L, "Again", EMAIL))
                .isInstanceOf(DepartmentApplicationException.class);
        verify(proformaInvoiceRepository).save(any());
        verify(applicationEventPublisher).publishEvent(any(DepartmentTaxInvoiceGenerationRequestedEvent.class));
    }

    @ParameterizedTest
    @EnumSource(value = DepartmentApplicationStatus.class, names = {"AUDITOR_APPROVED", "COMPLETED"})
    void approvedApplicationsCannotBeReviewedAgain(DepartmentApplicationStatus status) {
        application.setApplicationStatus(status);
        for (AuditorReviewDecision decision : AuditorReviewDecision.values()) {
            assertThatThrownBy(() -> service.reviewByAuditor(37L, decision, "Changed", EMAIL))
                    .isInstanceOf(DepartmentApplicationException.class).hasMessageContaining("final");
        }
        verify(applicationRepository, never()).save(any());
        verifyNoInteractions(activityRepository, proformaInvoiceRepository, applicationEventPublisher);
    }

    @Test
    void refreshRejectsAnApprovalCompletedByAnotherRequest() {
        doAnswer(invocation -> {
            application.setApplicationStatus(DepartmentApplicationStatus.COMPLETED);
            return null;
        }).when(entityManager).refresh(application, LockModeType.PESSIMISTIC_WRITE);

        assertThatThrownBy(() -> service.reviewByAuditor(37L, AuditorReviewDecision.APPROVE, "", EMAIL))
                .isInstanceOf(DepartmentApplicationException.class).hasMessageContaining("final");
        verify(applicationRepository, never()).save(any());
        verifyNoInteractions(activityRepository, proformaInvoiceRepository, applicationEventPublisher);
    }

    @Test
    void sendBackCannotBeSubmittedTwice() {
        when(applicationRepository.save(application)).thenReturn(application);
        assertThat(service.reviewByAuditor(37L, AuditorReviewDecision.SEND_BACK, "Correct details", EMAIL))
                .isEqualTo(DepartmentApplicationStatus.AUDITOR_SENT_BACK);
        assertThatThrownBy(() -> service.reviewByAuditor(37L, AuditorReviewDecision.SEND_BACK, "Again", EMAIL))
                .isInstanceOf(DepartmentApplicationException.class);
        verify(applicationRepository).save(application);
        verifyNoInteractions(proformaInvoiceRepository, applicationEventPublisher);
    }

    @Test
    void invoiceFailurePropagatesToRollBackApproval() {
        when(applicationRepository.save(application)).thenReturn(application);
        when(proformaInvoiceRepository.save(any())).thenThrow(new IllegalStateException("Invoice failure"));

        assertThatThrownBy(() -> service.reviewByAuditor(37L, AuditorReviewDecision.APPROVE, "", EMAIL))
                .isInstanceOf(DepartmentApplicationException.class).hasMessageContaining("No approval changes were saved");
        verifyNoInteractions(applicationEventPublisher);
    }
}
