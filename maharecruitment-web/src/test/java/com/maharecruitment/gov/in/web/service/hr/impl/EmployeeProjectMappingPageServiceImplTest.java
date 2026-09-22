package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProjectMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

class EmployeeProjectMappingPageServiceImplTest {

    private EmployeeRepository employeeRepository;
    private ProjectMstRepository projectRepository;
    private EmployeeProjectMappingRepository mappingRepository;
    private EmployeeProjectMappingPageServiceImpl service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        projectRepository = mock(ProjectMstRepository.class);
        mappingRepository = mock(EmployeeProjectMappingRepository.class);
        service = new EmployeeProjectMappingPageServiceImpl(
                employeeRepository,
                projectRepository,
                mappingRepository);
    }

    @Test
    void externalEmployeeCanBeMappedToExternalProject() {
        EmployeeEntity employee = employee(7L, "EXTERNAL");
        ProjectMst project = project(21L, ProjectScopeType.EXTERNAL);
        when(employeeRepository.findDetailedByEmployeeId(7L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(21L)).thenReturn(Optional.of(project));
        when(mappingRepository.findByEmployeeEmployeeId(7L)).thenReturn(Optional.empty());

        assertThat(service.updateMapping(7L, 21L)).isTrue();

        ArgumentCaptor<EmployeeProjectMappingEntity> mapping =
                ArgumentCaptor.forClass(EmployeeProjectMappingEntity.class);
        verify(mappingRepository).save(mapping.capture());
        assertThat(mapping.getValue().getEmployee()).isSameAs(employee);
        assertThat(mapping.getValue().getProject()).isSameAs(project);
    }

    @Test
    void externalEmployeeCannotBeMappedToInternalProject() {
        when(employeeRepository.findDetailedByEmployeeId(7L))
                .thenReturn(Optional.of(employee(7L, "EXTERNAL")));
        when(projectRepository.findById(22L))
                .thenReturn(Optional.of(project(22L, ProjectScopeType.INTERNAL)));

        assertThatThrownBy(() -> service.updateMapping(7L, 22L))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("External employees can only be mapped to external projects.");
        verify(mappingRepository, never()).save(any());
    }

    @Test
    void externalEmployeeEditScreenContainsOnlyExternalProjectsFromTheirDepartment() {
        EmployeeEntity employee = employee(7L, "EXTERNAL");
        ProjectMst externalProject = project(21L, ProjectScopeType.EXTERNAL);
        ProjectMst otherDepartmentProject = project(22L, ProjectScopeType.EXTERNAL);
        otherDepartmentProject.setDepartmentId(20L);
        ProjectMst otherSubDepartmentProject = project(23L, ProjectScopeType.EXTERNAL);
        otherSubDepartmentProject.setSubDepartmentId(99L);
        when(employeeRepository.findDetailedByEmployeeId(7L)).thenReturn(Optional.of(employee));
        when(mappingRepository.findByEmployeeEmployeeId(7L)).thenReturn(Optional.empty());
        when(projectRepository.findByProjectScopeTypeAndActiveFlagIgnoreCaseOrderByProjectNameAsc(
                ProjectScopeType.EXTERNAL,
                "Y")).thenReturn(List.of(externalProject, otherDepartmentProject, otherSubDepartmentProject));

        var view = service.loadMapping(7L);

        assertThat(view.requiredScope()).isEqualTo("EXTERNAL");
        assertThat(view.availableProjects())
                .extracting(project -> project.scope())
                .containsExactly("EXTERNAL");
        assertThat(view.availableProjects()).extracting(project -> project.projectId()).containsExactly(21L);
    }

    @Test
    void assignmentSearchUsesDatabasePaginationWithoutLoadingMappings() {
        var pageable = PageRequest.of(1, 25);
        EmployeeEntity employee = employee(7L, "EXTERNAL");
        when(employeeRepository.findActiveOnboardedWithoutProjectMapping(
                "EXTERNAL",
                null,
                null,
                null,
                "%TEST%",
                pageable)).thenReturn(new PageImpl<>(List.of(employee), pageable, 26));

        var result = service.searchUnmappedEmployees("external", " test ", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(26);
        assertThat(result.getContent()).singleElement()
                .satisfies(view -> assertThat(view.mappedProject()).isNull());
        verify(mappingRepository, never()).findByEmployeeEmployeeIdIn(any());
    }

    @Test
    void mappedSearchLoadsAllProjectsForThePageInOneBatch() {
        var pageable = PageRequest.of(0, 10);
        EmployeeEntity employee = employee(7L, "EXTERNAL");
        ProjectMst project = project(21L, ProjectScopeType.EXTERNAL);
        EmployeeProjectMappingEntity mapping = new EmployeeProjectMappingEntity();
        mapping.setEmployee(employee);
        mapping.setProject(project);
        when(employeeRepository.findActiveOnboardedWithProjectMapping(
                null,
                "%EXTERNAL PROJECT%",
                pageable)).thenReturn(new PageImpl<>(List.of(employee), pageable, 1));
        when(mappingRepository.findByEmployeeEmployeeIdIn(List.of(7L))).thenReturn(List.of(mapping));

        var result = service.searchMappedEmployees("ALL", "External Project", pageable);

        assertThat(result.getContent()).singleElement()
                .satisfies(view -> assertThat(view.mappedProject().projectId()).isEqualTo(21L));
        verify(mappingRepository).findByEmployeeEmployeeIdIn(List.of(7L));
    }

    @Test
    void bulkMappingAssignsMultipleExternalEmployeesToOneExternalProject() {
        EmployeeEntity first = employee(7L, "EXTERNAL");
        EmployeeEntity second = employee(8L, "EXTERNAL");
        ProjectMst project = project(21L, ProjectScopeType.EXTERNAL);
        when(projectRepository.findById(21L)).thenReturn(Optional.of(project));
        when(employeeRepository.findDetailedByEmployeeIdIn(List.of(7L, 8L)))
                .thenReturn(List.of(first, second));
        when(mappingRepository.findByEmployeeEmployeeIdIn(List.of(7L, 8L))).thenReturn(List.of());

        var result = service.updateMappings(21L, List.of(7L, 8L));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.changedCount()).isEqualTo(2);
        assertThat(result.unchangedCount()).isZero();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeProjectMappingEntity>> mappings = ArgumentCaptor.forClass(List.class);
        verify(mappingRepository).saveAll(mappings.capture());
        assertThat(mappings.getValue())
                .hasSize(2)
                .allSatisfy(mapping -> assertThat(mapping.getProject()).isSameAs(project));
    }

    @Test
    void bulkMappingRejectsMixedEmployeeScopesWithoutSavingAnything() {
        ProjectMst externalProject = project(21L, ProjectScopeType.EXTERNAL);
        when(projectRepository.findById(21L)).thenReturn(Optional.of(externalProject));
        when(employeeRepository.findDetailedByEmployeeIdIn(List.of(7L, 8L)))
                .thenReturn(List.of(employee(7L, "EXTERNAL"), employee(8L, "INTERNAL")));

        assertThatThrownBy(() -> service.updateMappings(21L, List.of(7L, 8L)))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Internal and MAHAIT employees can only be mapped to internal projects.");
        verify(mappingRepository, never()).saveAll(any());
    }

    @Test
    void bulkMappingRejectsDifferentDepartmentsWithoutSavingAnything() {
        EmployeeEntity first = employee(7L, "EXTERNAL");
        EmployeeEntity second = employee(8L, "EXTERNAL");
        second.getDepartment().setDepartmentId(20L);
        when(projectRepository.findById(21L)).thenReturn(Optional.of(project(21L, ProjectScopeType.EXTERNAL)));
        when(employeeRepository.findDetailedByEmployeeIdIn(List.of(7L, 8L)))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.updateMappings(21L, List.of(7L, 8L)))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Selected project belongs to a different department than the employee.");
        verify(mappingRepository, never()).saveAll(any());
    }

    @Test
    void employeeCanBeMappedToProjectInTheirSubDepartment() {
        EmployeeEntity employee = employee(7L, "EXTERNAL");
        SubDepartment subDepartment = new SubDepartment();
        subDepartment.setSubDeptId(15L);
        subDepartment.setDepartment(employee.getDepartment());
        employee.setSubDepartment(subDepartment);
        ProjectMst project = project(21L, ProjectScopeType.EXTERNAL);
        project.setSubDepartmentId(15L);
        when(employeeRepository.findDetailedByEmployeeId(7L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(21L)).thenReturn(Optional.of(project));

        assertThat(service.updateMapping(7L, 21L)).isTrue();
        verify(mappingRepository).save(any(EmployeeProjectMappingEntity.class));
    }

    @Test
    void matchingDepartmentDoesNotAllowProjectInDifferentSubDepartment() {
        EmployeeEntity employee = employee(7L, "EXTERNAL");
        SubDepartment subDepartment = new SubDepartment();
        subDepartment.setSubDeptId(15L);
        subDepartment.setDepartment(employee.getDepartment());
        employee.setSubDepartment(subDepartment);
        ProjectMst project = project(21L, ProjectScopeType.EXTERNAL);
        project.setSubDepartmentId(16L);
        when(employeeRepository.findDetailedByEmployeeId(7L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(21L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.updateMapping(7L, 21L))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Selected project belongs to a different subdepartment than the employee.");
        verify(mappingRepository, never()).save(any());
    }

    private EmployeeEntity employee(Long id, String recruitmentType) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(id);
        employee.setEmployeeCode("EMP-" + id);
        employee.setFullName("Test Employee");
        employee.setEmail("employee@example.com");
        employee.setRecruitmentType(recruitmentType);
        employee.setStatus("ACTIVE");
        DepartmentMst department = new DepartmentMst();
        department.setDepartmentId(10L);
        department.setDepartmentName("Test Department");
        employee.setDepartment(department);
        return employee;
    }

    private ProjectMst project(Long id, ProjectScopeType scope) {
        ProjectMst project = new ProjectMst();
        project.setProjectId(id);
        project.setProjectName(scope.name() + " Project");
        project.setProjectCode("PRJ-" + id);
        project.setProjectScopeType(scope);
        project.setActiveFlag("Y");
        project.setDepartmentId(10L);
        return project;
    }
}
