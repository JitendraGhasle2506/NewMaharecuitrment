package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.*;
import com.maharecruitment.gov.in.recruitment.repository.*;

class EmployeeExitServiceImplTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RecruitmentDesignationVacancyRepository vacancies = mock(RecruitmentDesignationVacancyRepository.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final EmployeeExitServiceImpl service = new EmployeeExitServiceImpl(employees, users, vacancies, audit);
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    private EmployeeEntity employee;
    private User account;

    @BeforeEach void setUp() {
        employee = new EmployeeEntity();
        employee.setEmployeeId(5L);
        employee.setEmployeeCode("EMP005");
        employee.setStatus("ACTIVE");
        account = new User();
        account.setId(50L);
        account.setActive(true);
        employee.setUser(account);
        User hr = new User();
        hr.setId(10L);
        hr.setEmail("hr@example.test");
        hr.setName("HR Officer");
        when(users.findByEmailIgnoreCaseAndActiveTrue("hr@example.test")).thenReturn(Optional.of(hr));
        when(employees.lockForExit(5L)).thenReturn(Optional.of(employee));
    }

    @Test void exitsImportedEmployeesOfEveryTypeWithoutDependingOnFormalities() {
        for (String type : new String[] { "INTERNAL", "MAHAIT", "EXTERNAL" }) {
            employee.setStatus("ACTIVE");
            employee.setRecruitmentType(type);
            account.setActive(true);
            service.exitEmployee(5L, today.minusDays(2), " Left the organisation ", "hr@example.test");
            assertThat(employee.getStatus()).isEqualTo("RESIGNED");
            assertThat(employee.getResignationDate()).isEqualTo(today.minusDays(2));
            assertThat(account.getActive()).isFalse();
        }
        verifyNoInteractions(vacancies);
        var captor = ArgumentCaptor.forClass(AuditRecordRequest.class);
        verify(audit, times(3)).record(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getActionType()).isEqualTo("HR_EMPLOYEE_EXITED");
        assertThat(captor.getValue().getMetadata()).containsEntry("reason", "Left the organisation")
                .containsEntry("previousAccountActive", true).containsEntry("accountActive", false);
    }

    @Test void exitsEmployeeWithoutLinkedAccount() {
        employee.setUser(null);
        service.exitEmployee(5L, today, "Employee left", "hr@example.test");
        assertThat(employee.getStatus()).isEqualTo("RESIGNED");
        verify(users, never()).save(any());
        verify(audit).record(any());
    }

    @Test void canDisableAnAccountLeftActiveByTheOldResignationFlow() {
        employee.setStatus("RESIGNED");
        service.exitEmployee(5L, today, "Disable access after exit", "hr@example.test");
        assertThat(account.getActive()).isFalse();
        verifyNoInteractions(vacancies);
    }

    @Test void rejectsDuplicateExitAndDoesNotReleaseVacancyTwice() {
        employee.setStatus("RESIGNED");
        account.setActive(false);
        assertThatThrownBy(() -> service.exitEmployee(5L, today, "Employee left", "hr@example.test"))
                .hasMessageContaining("already exited");
        verifyNoInteractions(audit, vacancies);
        verify(employees, never()).save(any());
    }

    @Test void rejectsInvalidDatesAndReasonsBeforeAnyWrites() {
        for (LocalDate invalid : new LocalDate[] { null, today.plusDays(1) }) {
            assertThatThrownBy(() -> service.exitEmployee(5L, invalid, "Employee left", "hr@example.test"))
                    .hasMessageContaining("cannot be in the future");
        }
        for (String invalid : new String[] { null, "  ", "x".repeat(1001) }) {
            assertThatThrownBy(() -> service.exitEmployee(5L, today, invalid, "hr@example.test"))
                    .hasMessageContaining("exit reason");
        }
        verifyNoInteractions(audit, vacancies);
        verify(employees, never()).save(any());
    }

    @Test void releasesAnExistingVacancyOnceWithoutNegativeCounts() {
        var vacancy = new RecruitmentDesignationVacancyEntity();
        vacancy.setRecruitmentDesignationVacancyId(20L);
        vacancy.setFillPost(1L);
        var notification = new RecruitmentNotificationEntity();
        notification.setRecruitmentNotificationId(30L);
        var interview = new RecruitmentInterviewDetailEntity();
        interview.setDesignationVacancy(vacancy);
        interview.setRecruitmentNotification(notification);
        var onboarding = new AgencyCandidatePreOnboardingEntity();
        onboarding.setInterviewDetail(interview);
        employee.setPreOnboarding(onboarding);
        when(vacancies.findByIdForFinalDecisionUpdate(20L, 30L)).thenReturn(Optional.of(vacancy));
        service.exitEmployee(5L, today, "Employee left", "hr@example.test");
        assertThat(vacancy.getFillPost()).isZero();
        verify(vacancies).save(vacancy);
        assertThatThrownBy(() -> service.exitEmployee(5L, today, "Employee left", "hr@example.test"))
                .hasMessageContaining("already exited");
        verify(vacancies, times(1)).save(vacancy);
    }

    @Test void inactiveActorCannotExitEmployees() {
        when(users.findByEmailIgnoreCaseAndActiveTrue("hr@example.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.exitEmployee(5L, today, "Employee left", "hr@example.test"))
                .hasMessageContaining("active HR account");
        verifyNoInteractions(audit, vacancies);
        verify(employees, never()).save(any());
    }
}