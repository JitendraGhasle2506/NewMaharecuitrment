package com.maharecruitment.gov.in.master.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.master.dto.ProjectRequest;
import com.maharecruitment.gov.in.master.dto.ProjectResponse;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;
import com.maharecruitment.gov.in.master.entity.ProjectType;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.mapper.ProjectMapper;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;

@ExtendWith(MockitoExtension.class)
class ProjectMstServiceImplTest {

    @Mock
    private ProjectMstRepository projectRepository;

    @Mock
    private CellMasterRepository cellRepository;

    @Mock
    private DepartmentMstRepository departmentRepository;

    @Mock
    private SubDepartmentRepository subDepartmentRepository;

    private ProjectMstServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProjectMstServiceImpl(
                projectRepository,
                cellRepository,
                departmentRepository,
                subDepartmentRepository,
                new ProjectMapper());
    }

    @Test
    void createStoresCanonicalDepartmentAndSubDepartmentIds() {
        DepartmentMst department = department(10L, "Finance");
        SubDepartment subDepartment = subDepartment(20L, "Accounts", department);
        CellMaster cell = activeCell(30L);
        ProjectRequest request = validRequest(10L, 20L, 30L);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(subDepartmentRepository.findBySubDeptIdAndDepartmentDepartmentId(20L, 10L))
                .thenReturn(Optional.of(subDepartment));
        when(cellRepository.findByCellId(30L)).thenReturn(Optional.of(cell));
        when(projectRepository.save(any(ProjectMst.class))).thenAnswer(invocation -> {
            ProjectMst project = invocation.getArgument(0);
            project.setProjectId(40L);
            return project;
        });

        ProjectResponse response = service.create(request);

        ArgumentCaptor<ProjectMst> captor = ArgumentCaptor.forClass(ProjectMst.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getDepartmentId()).isEqualTo(10L);
        assertThat(captor.getValue().getSubDepartmentId()).isEqualTo(20L);
        assertThat(response.getDepartmentId()).isEqualTo(10L);
        assertThat(response.getDepartmentName()).isEqualTo("Finance");
        assertThat(response.getSubDepartmentId()).isEqualTo(20L);
        assertThat(response.getSubDepartmentName()).isEqualTo("Accounts");
    }

    @Test
    void createRejectsSubDepartmentFromAnotherDepartment() {
        DepartmentMst department = department(10L, "Finance");
        ProjectRequest request = validRequest(10L, 99L, 30L);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(subDepartmentRepository.findBySubDeptIdAndDepartmentDepartmentId(99L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Selected sub-department does not belong to the selected department.");

        verify(projectRepository, never()).save(any(ProjectMst.class));
    }

    @Test
    void departmentApplicationSyncUsesDepartmentAndSubDepartmentIds() {
        DepartmentMst department = department(10L, "Finance");
        SubDepartment subDepartment = subDepartment(20L, "Accounts", department);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(subDepartmentRepository.findBySubDeptIdAndDepartmentDepartmentId(20L, 10L))
                .thenReturn(Optional.of(subDepartment));
        when(projectRepository.findFirstByApplicationId(50L)).thenReturn(Optional.empty());
        when(projectRepository.findFirstByProjectNameIgnoreCaseAndDepartmentIdAndSubDepartmentId(
                "Payroll", 10L, 20L)).thenReturn(Optional.empty());
        when(projectRepository.save(any(ProjectMst.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = service.upsertFromDepartmentApplication(
                " Payroll ", ProjectType.NEW_DEVELOPMENT, 10L, 20L, 50L);

        assertThat(response.getProjectName()).isEqualTo("Payroll");
        assertThat(response.getDepartmentId()).isEqualTo(10L);
        assertThat(response.getSubDepartmentId()).isEqualTo(20L);
        assertThat(response.getApplicationId()).isEqualTo(50L);
    }

    private ProjectRequest validRequest(Long departmentId, Long subDepartmentId, Long cellId) {
        ProjectRequest request = new ProjectRequest();
        request.setProjectName("Payroll");
        request.setProjectCode("PAY");
        request.setProjectType(ProjectType.NEW_DEVELOPMENT);
        request.setProjectScopeType(ProjectScopeType.INTERNAL);
        request.setDepartmentId(departmentId);
        request.setSubDepartmentId(subDepartmentId);
        request.setCellId(cellId);
        return request;
    }

    private DepartmentMst department(Long id, String name) {
        DepartmentMst department = new DepartmentMst();
        department.setDepartmentId(id);
        department.setDepartmentName(name);
        return department;
    }

    private SubDepartment subDepartment(Long id, String name, DepartmentMst department) {
        SubDepartment subDepartment = new SubDepartment();
        subDepartment.setSubDeptId(id);
        subDepartment.setSubDeptName(name);
        subDepartment.setDepartment(department);
        return subDepartment;
    }

    private CellMaster activeCell(Long id) {
        WingMaster wing = new WingMaster();
        wing.setWingId(1L);
        wing.setWingName("Operations");
        wing.setActiveFlag("Y");

        CellMaster cell = new CellMaster();
        cell.setCellId(id);
        cell.setCellName("Delivery");
        cell.setActiveFlag("Y");
        cell.setWing(wing);
        return cell;
    }
}
