package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamMappingRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeTeamMappingResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationAuditResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationLookupOption;
import com.maharecruitment.gov.in.recruitment.dto.organization.PositionRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.PositionResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamRequest;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamByCellResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.TeamResponse;

public interface OrganizationManagementService {

    TeamResponse createTeam(TeamRequest request);

    TeamResponse updateTeam(Long teamId, TeamRequest request);

    TeamResponse getTeam(Long teamId);

    Page<TeamResponse> searchTeams(
            Long projectId,
            Long cellId,
            boolean includeInactive,
            String search,
            Pageable pageable);

    void changeTeamStatus(Long teamId, boolean active);

    PositionResponse createPosition(PositionRequest request);

    PositionResponse updatePosition(Long positionId, PositionRequest request);

    PositionResponse getPosition(Long positionId);

    Page<PositionResponse> searchPositions(
            Long projectId,
            Long cellId,
            Long teamId,
            boolean includeInactive,
            String search,
            Pageable pageable);

    void changePositionStatus(Long positionId, boolean active);

    EmployeeTeamMappingResponse createMapping(EmployeeTeamMappingRequest request);

    EmployeeTeamMappingResponse updateMapping(Long mappingId, EmployeeTeamMappingRequest request);

    EmployeeTeamMappingResponse getMapping(Long mappingId);

    Page<EmployeeTeamMappingResponse> searchMappings(
            Long projectId,
            Long cellId,
            Long teamId,
            boolean includeInactive,
            String search,
            Pageable pageable);

    void deactivateMapping(Long mappingId);

    List<OrganizationLookupOption> getProjectOptions();

    List<OrganizationLookupOption> getCellOptions();

    List<OrganizationLookupOption> getTeamOptions(Long projectId, Long cellId);

    List<TeamByCellResponse> getActiveTeamsByCell(Long cellId);

    List<OrganizationLookupOption> getPositionOptions(Long projectId, Long teamId);

    List<OrganizationLookupOption> getDesignationOptions();

    List<OrganizationLookupOption> getLevelOptions(Long designationId);

    Page<OrganizationLookupOption> getEmployeeOptions(
            Long positionId,
            Long designationId,
            String levelCode,
            String search,
            Pageable pageable);

    List<OrganizationAuditResponse> getAuditTimeline(String entityType, String entityId);

    void seedSampleHierarchy(Long projectId);
}
