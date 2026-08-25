package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationMasterService;
import com.maharecruitment.gov.in.master.service.ManpowerDesignationRateService;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyHiringRequestType;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyOpeningRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyApprovalDocumentStorageService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentRequestIdGenerator;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyOpeningCommand;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyRequirementCommand;

class InternalVacancyOpeningServiceImplTest {

    private ProjectMstRepository projectRepository;
    private EmployeeRepository employeeRepository;
    private InternalVacancyOpeningServiceImpl service;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectMstRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new InternalVacancyOpeningServiceImpl(
                mock(InternalVacancyOpeningRepository.class),
                projectRepository,
                mock(ManpowerDesignationMasterRepository.class),
                mock(ManpowerDesignationMasterService.class),
                mock(ManpowerDesignationRateService.class),
                mock(RoleRepository.class),
                mock(UserRepository.class),
                employeeRepository,
                mock(RecruitmentRequestIdGenerator.class),
                mock(RecruitmentNotificationService.class),
                mock(InternalVacancyApprovalDocumentStorageService.class));

        when(projectRepository.findByProjectIdAndProjectScopeType(10L, ProjectScopeType.INTERNAL))
                .thenReturn(Optional.of(mock(ProjectMst.class)));
    }

    @Test
    void requiresEOfficeApprovalWhenSubmittingNewCandidateRequest() {
        InternalVacancyOpeningCommand command = baseCommand()
                .hiringRequestType(InternalVacancyHiringRequestType.NEW_CANDIDATE)
                .build();

        assertThatThrownBy(() -> service.saveOpening(command))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("E-office approval document is required for a new candidate request.");
    }

    @Test
    void requiresEmployeeSelectionWhenSubmittingReplacementRequest() {
        InternalVacancyOpeningCommand command = baseCommand()
                .hiringRequestType(InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT)
                .build();

        assertThatThrownBy(() -> service.saveOpening(command))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Select at least one employee to be replaced.");
    }

    @Test
    void rejectsInactiveReplacementEmployee() {
        EmployeeEntity inactiveEmployee = new EmployeeEntity();
        inactiveEmployee.setEmployeeId(99L);
        inactiveEmployee.setStatus("INACTIVE");
        when(employeeRepository.findReplacementEmployeesByEmployeeIdIn(anyCollection()))
                .thenReturn(List.of(inactiveEmployee));

        InternalVacancyOpeningCommand command = baseCommand()
                .hiringRequestType(InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT)
                .replacementEmployeeIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> service.saveOpening(command))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Selected replacement employees must be active.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void groupsReplacementVacanciesByDesignationAndLevel() {
        ManpowerDesignationMaster developer = ManpowerDesignationMaster.builder()
                .designationId(1L)
                .designationName("Developer")
                .build();
        ManpowerDesignationMaster tester = ManpowerDesignationMaster.builder()
                .designationId(2L)
                .designationName("Tester")
                .build();

        List<EmployeeEntity> employees = List.of(
                replacementEmployee(11L, developer, "L1"),
                replacementEmployee(12L, developer, "l1"),
                replacementEmployee(13L, developer, "L2"),
                replacementEmployee(14L, tester, "L1"));

        List<InternalVacancyRequirementCommand> requirements = ReflectionTestUtils.invokeMethod(
                service,
                "buildReplacementRequirements",
                employees);

        assertThat(requirements)
                .extracting(
                        InternalVacancyRequirementCommand::getDesignationId,
                        InternalVacancyRequirementCommand::getLevelCode,
                        InternalVacancyRequirementCommand::getNumberOfVacancy)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "L1", 2L),
                        org.assertj.core.groups.Tuple.tuple(1L, "L2", 1L),
                        org.assertj.core.groups.Tuple.tuple(2L, "L1", 1L));
    }

    @Test
    void replacementEmployeeLabelDoesNotExposeEmployeeCode() {
        ManpowerDesignationMaster developer = ManpowerDesignationMaster.builder()
                .designationId(1L)
                .designationName("Developer")
                .build();
        EmployeeEntity employee = replacementEmployee(11L, developer, "L1");
        employee.setFullName("Employee Name");
        employee.setEmployeeCode("MahaIT3286");

        String label = ReflectionTestUtils.invokeMethod(service, "buildReplacementEmployeeLabel", employee);

        assertThat(label)
                .isEqualTo("Employee Name - Developer - L1")
                .doesNotContain("MahaIT3286");
    }

    private EmployeeEntity replacementEmployee(
            Long employeeId,
            ManpowerDesignationMaster designation,
            String levelCode) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setDesignation(designation);
        employee.setLevelCode(levelCode);
        return employee;
    }

    private InternalVacancyOpeningCommand.InternalVacancyOpeningCommandBuilder baseCommand() {
        return InternalVacancyOpeningCommand.builder()
                .projectId(10L)
                .actorEmail("hod@example.com")
                .targetStatus(InternalVacancyOpeningStatus.PENDING_HR_APPROVAL)
                .requirements(List.of(InternalVacancyRequirementCommand.builder()
                        .designationId(1L)
                        .levelCode("L1")
                        .numberOfVacancy(1L)
                        .build()))
                .interviewAuthorityRoleIds(List.of())
                .interviewAuthorityUserIds(List.of())
                .interviewAuthorityEmployeeIds(List.of());
    }
}
