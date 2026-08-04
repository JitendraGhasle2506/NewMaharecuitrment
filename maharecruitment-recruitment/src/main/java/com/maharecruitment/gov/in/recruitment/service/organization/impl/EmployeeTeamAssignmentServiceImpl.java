package com.maharecruitment.gov.in.recruitment.service.organization.impl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamAssignmentResponse;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.EmployeeTeamMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.TeamMasterEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.EmployeeTeamMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.organization.OrganizationTeamRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.EmployeeTeamAssignmentService;

@Service
@Transactional(readOnly = true)
public class EmployeeTeamAssignmentServiceImpl implements EmployeeTeamAssignmentService {

    private static final String INTERNAL = "INTERNAL";
    private static final String ACTIVE = "ACTIVE";

    private final EmployeeTeamMappingRepository mappingRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationTeamRepository teamRepository;

    public EmployeeTeamAssignmentServiceImpl(
            EmployeeTeamMappingRepository mappingRepository,
            EmployeeRepository employeeRepository,
            OrganizationTeamRepository teamRepository) {
        this.mappingRepository = mappingRepository;
        this.employeeRepository = employeeRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public List<EmployeeTeamAssignmentResponse> getAssignments() {
        Map<Long, EmployeeTeamMappingEntity> latestByEmployeeId = new LinkedHashMap<>();
        mappingRepository.findAllByStatusWithTeam(OrganizationRecordStatus.ACTIVE).forEach(mapping -> {
            if (mapping.getEmployee() != null && mapping.getEmployee().getEmployeeId() != null) {
                latestByEmployeeId.putIfAbsent(mapping.getEmployee().getEmployeeId(), mapping);
            }
        });

        return employeeRepository
                .findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(INTERNAL, ACTIVE)
                .stream()
                .map(employee -> toResponse(employee, latestByEmployeeId.get(employee.getEmployeeId())))
                .toList();
    }

    @Override
    @Transactional
    public EmployeeTeamAssignmentResponse assignTeam(Long employeeId, Long teamId) {
        EmployeeEntity employee = getActiveInternalEmployee(employeeId);
        TeamMasterEntity team = getActiveTeam(teamId);
        List<EmployeeTeamMappingEntity> activeMappings = mappingRepository
                .findByEmployee_EmployeeIdAndStatus(employeeId, OrganizationRecordStatus.ACTIVE);

        if (activeMappings.size() == 1 && sameTeam(activeMappings.getFirst(), teamId)) {
            return toResponse(employee, activeMappings.getFirst());
        }

        deactivate(activeMappings);
        EmployeeTeamMappingEntity mapping = new EmployeeTeamMappingEntity();
        mapping.setEmployee(employee);
        mapping.setTeam(team);
        mapping.setPosition(null);
        mapping.setEffectiveDate(LocalDate.now());
        mapping.setStatus(OrganizationRecordStatus.ACTIVE);
        return toResponse(employee, mappingRepository.save(mapping));
    }

    @Override
    @Transactional
    public void clearTeam(Long employeeId) {
        getActiveInternalEmployee(employeeId);
        deactivate(mappingRepository.findByEmployee_EmployeeIdAndStatus(
                employeeId,
                OrganizationRecordStatus.ACTIVE));
    }

    private EmployeeEntity getActiveInternalEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessValidationException("Employee is required.");
        }
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for id: " + employeeId));
        if (!ACTIVE.equalsIgnoreCase(employee.getStatus())
                || !INTERNAL.equalsIgnoreCase(employee.getRecruitmentType())) {
            throw new BusinessValidationException("Only an active internal employee can be assigned.");
        }
        return employee;
    }

    private TeamMasterEntity getActiveTeam(Long teamId) {
        if (teamId == null) {
            throw new BusinessValidationException("Team is required.");
        }
        TeamMasterEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found for id: " + teamId));
        if (team.getStatus() != OrganizationRecordStatus.ACTIVE) {
            throw new BusinessValidationException("Only an active team can be assigned.");
        }
        if (team.getCell() == null) {
            throw new BusinessValidationException("Selected team is not mapped to a cell.");
        }
        return team;
    }

    private void deactivate(List<EmployeeTeamMappingEntity> mappings) {
        if (mappings.isEmpty()) {
            return;
        }
        mappings.forEach(mapping -> mapping.setStatus(OrganizationRecordStatus.INACTIVE));
        mappingRepository.saveAll(mappings);
    }

    private boolean sameTeam(EmployeeTeamMappingEntity mapping, Long teamId) {
        return mapping.getTeam() != null && teamId.equals(mapping.getTeam().getTeamId());
    }

    private EmployeeTeamAssignmentResponse toResponse(
            EmployeeEntity employee,
            EmployeeTeamMappingEntity mapping) {
        TeamMasterEntity team = mapping == null ? null : mapping.getTeam();
        CellMaster cell = team == null ? null : team.getCell();
        return new EmployeeTeamAssignmentResponse(
                mapping == null ? null : mapping.getMappingId(),
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getDesignation() == null ? null : employee.getDesignation().getDesignationName(),
                employee.getStatus(),
                team == null ? null : team.getTeamId(),
                team == null ? null : team.getTeamName(),
                team == null ? null : team.getTeamType(),
                cell == null ? null : cell.getCellId(),
                cell == null ? null : cell.getCellName(),
                cell == null || cell.getWing() == null ? null : cell.getWing().getWingName());
    }
}
