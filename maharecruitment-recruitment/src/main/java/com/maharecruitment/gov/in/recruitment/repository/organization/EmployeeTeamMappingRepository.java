package com.maharecruitment.gov.in.recruitment.repository.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.organization.EmployeeTeamMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;

@Repository
public interface EmployeeTeamMappingRepository extends JpaRepository<EmployeeTeamMappingEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "employee",
            "team",
            "team.project",
            "team.cell",
            "position",
            "position.project",
            "position.cell",
            "position.designation",
            "position.resourceLevel" })
    Optional<EmployeeTeamMappingEntity> findById(Long mappingId);

    @EntityGraph(attributePaths = {
            "employee",
            "team",
            "team.project",
            "team.cell",
            "position",
            "position.project",
            "position.cell",
            "position.designation",
            "position.resourceLevel" })
    @Query("select m from EmployeeTeamMappingEntity m "
            + "left join m.team team "
            + "left join team.project teamProject "
            + "left join team.cell teamCell "
            + "left join m.position position "
            + "left join position.project positionProject "
            + "left join position.cell positionCell "
            + "where (:projectId is null "
            + "or teamProject.projectId = :projectId "
            + "or positionProject.projectId = :projectId "
            + "or (teamProject is null and positionProject is null "
            + "and exists (select 1 from ProjectMst selectedProject "
            + "where selectedProject.projectId = :projectId "
            + "and selectedProject.cell.cellId = teamCell.cellId))) "
            + "and (:cellId is null "
            + "or teamCell.cellId = :cellId "
            + "or positionCell.cellId = :cellId) "
            + "and (:teamId is null or team.teamId = :teamId) "
            + "and (:includeInactive = true or m.status = :activeStatus) "
            + "and (:search is null "
            + "or lower(coalesce(m.employee.fullName, '')) like :search "
            + "or lower(coalesce(m.employee.employeeCode, '')) like :search "
            + "or lower(team.teamName) like :search "
            + "or lower(position.positionName) like :search "
            + "or lower(position.designation.designationName) like :search "
            + "or lower(coalesce(teamProject.projectName, '')) like :search "
            + "or lower(coalesce(positionProject.projectName, '')) like :search) "
            + "order by m.effectiveDate desc, m.mappingId desc")
    Page<EmployeeTeamMappingEntity> searchMappings(
            @Param("projectId") Long projectId,
            @Param("cellId") Long cellId,
            @Param("teamId") Long teamId,
            @Param("includeInactive") boolean includeInactive,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("search") String search,
            Pageable pageable);

    @EntityGraph(attributePaths = { "employee", "team", "position", "position.project" })
    Optional<EmployeeTeamMappingEntity> findFirstByPosition_PositionIdAndStatusOrderByEffectiveDateDescMappingIdDesc(
            Long positionId,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "employee", "team", "position", "position.project" })
    List<EmployeeTeamMappingEntity> findByEmployee_EmployeeIdAndStatus(
            Long employeeId,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "employee", "employee.designation", "team", "team.cell", "team.cell.wing" })
    @Query("select mapping from EmployeeTeamMappingEntity mapping "
            + "where mapping.status = :status "
            + "order by mapping.effectiveDate desc, mapping.mappingId desc")
    List<EmployeeTeamMappingEntity> findAllByStatusWithTeam(
            @Param("status") OrganizationRecordStatus status);

    boolean existsByPosition_PositionIdAndStatusAndMappingIdNot(
            Long positionId,
            OrganizationRecordStatus status,
            Long mappingId);
}
