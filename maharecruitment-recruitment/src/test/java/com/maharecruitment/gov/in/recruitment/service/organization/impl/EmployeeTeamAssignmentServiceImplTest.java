package com.maharecruitment.gov.in.recruitment.service.organization.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamAssignmentResponse;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.EmployeeTeamMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationTeamType;
import com.maharecruitment.gov.in.recruitment.entity.organization.TeamMasterEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.EmployeeTeamMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.OrganizationTeamRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeTeamAssignmentServiceImplTest {

    @Mock
    private EmployeeTeamMappingRepository mappingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationTeamRepository teamRepository;

    @InjectMocks
    private EmployeeTeamAssignmentServiceImpl service;

    @Test
    void getAssignmentsReturnsMappedAndUnmappedActiveEmployees() {
        EmployeeEntity mappedEmployee = employee(41L, "Asha Patil");
        EmployeeEntity unmappedEmployee = employee(42L, "Mohan Jadhav");
        EmployeeTeamMappingEntity mapping = mapping(12L, mappedEmployee, team(9L));
        when(mappingRepository.findAllByStatusWithTeam(OrganizationRecordStatus.ACTIVE))
                .thenReturn(List.of(mapping));
        when(employeeRepository.findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
                "INTERNAL", "ACTIVE"))
                .thenReturn(List.of(mappedEmployee, unmappedEmployee));

        List<EmployeeTeamAssignmentResponse> result = service.getAssignments();

        assertEquals(2, result.size());
        assertEquals(9L, result.getFirst().teamId());
        assertEquals(5L, result.getFirst().cellId());
        assertNull(result.get(1).mappingId());
        assertNull(result.get(1).teamId());
    }

    @Test
    void assignTeamClosesPreviousMappingAndCreatesPositionFreeHistoryRow() {
        EmployeeEntity employee = employee(41L, "Asha Patil");
        EmployeeTeamMappingEntity previous = mapping(12L, employee, team(8L));
        TeamMasterEntity selectedTeam = team(9L);
        when(employeeRepository.findById(41L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(9L)).thenReturn(Optional.of(selectedTeam));
        when(mappingRepository.findByEmployee_EmployeeIdAndStatus(41L, OrganizationRecordStatus.ACTIVE))
                .thenReturn(List.of(previous));
        when(mappingRepository.save(any(EmployeeTeamMappingEntity.class))).thenAnswer(invocation -> {
            EmployeeTeamMappingEntity saved = invocation.getArgument(0);
            saved.setMappingId(13L);
            return saved;
        });

        EmployeeTeamAssignmentResponse result = service.assignTeam(41L, 9L);

        assertEquals(OrganizationRecordStatus.INACTIVE, previous.getStatus());
        verify(mappingRepository).saveAll(List.of(previous));
        ArgumentCaptor<EmployeeTeamMappingEntity> captor = ArgumentCaptor.forClass(EmployeeTeamMappingEntity.class);
        verify(mappingRepository).save(captor.capture());
        assertEquals(41L, captor.getValue().getEmployee().getEmployeeId());
        assertEquals(9L, captor.getValue().getTeam().getTeamId());
        assertNull(captor.getValue().getPosition());
        assertEquals(LocalDate.now(), captor.getValue().getEffectiveDate());
        assertEquals(13L, result.mappingId());
    }

    @Test
    void assignTeamRejectsInactiveTeam() {
        EmployeeEntity employee = employee(41L, "Asha Patil");
        TeamMasterEntity inactiveTeam = team(9L);
        inactiveTeam.setStatus(OrganizationRecordStatus.INACTIVE);
        when(employeeRepository.findById(41L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(9L)).thenReturn(Optional.of(inactiveTeam));

        assertThrows(BusinessValidationException.class, () -> service.assignTeam(41L, 9L));

        verify(mappingRepository, never()).save(any());
    }

    @Test
    void clearTeamDeactivatesAllActiveEmployeeMappings() {
        EmployeeEntity employee = employee(41L, "Asha Patil");
        EmployeeTeamMappingEntity mapping = mapping(12L, employee, team(9L));
        when(employeeRepository.findById(41L)).thenReturn(Optional.of(employee));
        when(mappingRepository.findByEmployee_EmployeeIdAndStatus(41L, OrganizationRecordStatus.ACTIVE))
                .thenReturn(List.of(mapping));

        service.clearTeam(41L);

        assertEquals(OrganizationRecordStatus.INACTIVE, mapping.getStatus());
        verify(mappingRepository).saveAll(List.of(mapping));
    }

    private EmployeeEntity employee(Long employeeId, String name) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP-" + employeeId);
        employee.setFullName(name);
        employee.setRecruitmentType("INTERNAL");
        employee.setStatus("ACTIVE");
        return employee;
    }

    private EmployeeTeamMappingEntity mapping(
            Long mappingId,
            EmployeeEntity employee,
            TeamMasterEntity team) {
        EmployeeTeamMappingEntity mapping = new EmployeeTeamMappingEntity();
        mapping.setMappingId(mappingId);
        mapping.setEmployee(employee);
        mapping.setTeam(team);
        mapping.setEffectiveDate(LocalDate.now());
        mapping.setStatus(OrganizationRecordStatus.ACTIVE);
        return mapping;
    }

    private TeamMasterEntity team(Long teamId) {
        WingMaster wing = WingMaster.builder()
                .wingId(3L)
                .wingName("Technology")
                .build();
        CellMaster cell = CellMaster.builder()
                .cellId(5L)
                .cellName("Application Development")
                .wing(wing)
                .build();
        TeamMasterEntity team = new TeamMasterEntity();
        team.setTeamId(teamId);
        team.setTeamName("Citizen Services");
        team.setTeamType(OrganizationTeamType.DEVELOPMENT);
        team.setCell(cell);
        team.setStatus(OrganizationRecordStatus.ACTIVE);
        return team;
    }
}
