package com.maharecruitment.gov.in.recruitment.repository.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionMasterEntity;
import com.maharecruitment.gov.in.recruitment.entity.organization.PositionStatus;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;

@Repository
public interface PositionMasterRepository extends JpaRepository<PositionMasterEntity, Long> {

    @Override
    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    Optional<PositionMasterEntity> findById(Long positionId);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    @Query("select p from PositionMasterEntity p "
            + "left join p.project project "
            + "join p.cell cell "
            + "left join p.team team "
            + "left join p.resourceLevel level "
            + "left join p.employee employee "
            + "where (:projectId is null "
            + "or project.projectId = :projectId "
            + "or (project is null and exists (select 1 from ProjectMst selectedProject "
            + "where selectedProject.projectId = :projectId "
            + "and selectedProject.cell.cellId = cell.cellId))) "
            + "and (:cellId is null or cell.cellId = :cellId) "
            + "and (:teamId is null or team.teamId = :teamId) "
            + "and (:includeInactive = true or p.status = :activeStatus) "
            + "and (:search is null "
            + "or lower(p.positionName) like :search "
            + "or lower(p.designation.designationName) like :search "
            + "or lower(coalesce(employee.fullName, '')) like :search "
            + "or lower(coalesce(team.teamName, '')) like :search "
            + "or lower(cell.cellName) like :search "
            + "or lower(coalesce(level.levelCode, '')) like :search "
            + "or lower(coalesce(level.levelName, '')) like :search "
            + "or lower(coalesce(project.projectName, '')) like :search "
            + "or lower(coalesce(project.projectCode, '')) like :search) "
            + "order by cell.cellName asc, coalesce(team.teamName, '') asc, p.displayOrder asc, p.positionId asc")
    Page<PositionMasterEntity> searchPositions(
            @Param("projectId") Long projectId,
            @Param("cellId") Long cellId,
            @Param("teamId") Long teamId,
            @Param("includeInactive") boolean includeInactive,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("search") String search,
            Pageable pageable);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    List<PositionMasterEntity> findByProject_ProjectIdAndStatusOrderByDisplayOrderAscPositionIdAsc(
            Long projectId,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    List<PositionMasterEntity> findByProject_ProjectIdInAndStatusOrderByDisplayOrderAscPositionIdAsc(
            Collection<Long> projectIds,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    @Query("select p from PositionMasterEntity p "
            + "join p.cell cell "
            + "where p.status = :status "
            + "order by cell.cellName asc, p.displayOrder asc, p.positionId asc")
    List<PositionMasterEntity> findByStatusOrderByCell_CellNameAscDisplayOrderAscPositionIdAsc(
            @Param("status") OrganizationRecordStatus status);

    @Query("""
            select cell.cellId as cellId,
                   count(distinct employee.employeeId) as employeeCount
            from PositionMasterEntity p
            join p.cell cell
            join p.employee employee
            where p.status = :activeStatus
              and p.positionStatus = :filledStatus
              and upper(trim(coalesce(employee.status, ''))) = :employeeStatus
            group by cell.cellId
            """)
    List<PositionCellEmployeeCountProjection> summarizeFilledActiveEmployeesByCell(
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("filledStatus") PositionStatus filledStatus,
            @Param("employeeStatus") String employeeStatus);

    @Query("""
            select cell.cellId as cellId,
                   count(distinct employee.employeeId) as employeeCount
            from PositionMasterEntity p
            join p.cell cell
            join cell.wing wing
            join p.employee employee
            where wing.wingId = :wingId
              and p.status = :activeStatus
              and p.positionStatus = :filledStatus
              and upper(trim(coalesce(employee.status, ''))) = :employeeStatus
            group by cell.cellId
            """)
    List<PositionCellEmployeeCountProjection> summarizeFilledActiveEmployeesByCellAndWingId(
            @Param("wingId") Long wingId,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("filledStatus") PositionStatus filledStatus,
            @Param("employeeStatus") String employeeStatus);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    @Query("select p from PositionMasterEntity p "
            + "left join p.project project "
            + "join p.cell cell "
            + "where p.status = :status "
            + "and (:projectId is null "
            + "or project.projectId = :projectId "
            + "or (project is null and :cellId is not null and cell.cellId = :cellId)) "
            + "order by cell.cellName asc, p.displayOrder asc, p.positionId asc")
    List<PositionMasterEntity> findByProjectScopeAndStatusOrderByDisplayOrderAscPositionIdAsc(
            @Param("projectId") Long projectId,
            @Param("cellId") Long cellId,
            @Param("status") OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    @Query("select p from PositionMasterEntity p "
            + "join p.cell cell "
            + "where p.status = :status "
            + "and (:cellId is null or cell.cellId = :cellId) "
            + "order by p.displayOrder asc, p.positionId asc")
    List<PositionMasterEntity> findByCellScopeAndStatusOrderByDisplayOrderAscPositionIdAsc(
            @Param("cellId") Long cellId,
            @Param("status") OrganizationRecordStatus status);

    long countByProject_ProjectIdAndStatus(Long projectId, OrganizationRecordStatus status);

    long countByProject_ProjectIdAndStatusAndPositionStatus(
            Long projectId,
            OrganizationRecordStatus status,
            PositionStatus positionStatus);

    long countByStatus(OrganizationRecordStatus status);

    long countByStatusAndPositionStatus(
            OrganizationRecordStatus status,
            PositionStatus positionStatus);

    long countByCell_CellIdAndStatus(
            Long cellId,
            OrganizationRecordStatus status);

    long countByCell_CellIdAndStatusAndPositionStatus(
            Long cellId,
            OrganizationRecordStatus status,
            PositionStatus positionStatus);

    long countByTeam_TeamId(Long teamId);

    boolean existsByReportingPosition_PositionIdAndStatus(Long reportingPositionId, OrganizationRecordStatus status);

    List<PositionMasterEntity> findByEmployee_EmployeeIdAndStatusAndPositionStatus(
            Long employeeId,
            OrganizationRecordStatus status,
            PositionStatus positionStatus);

    @Query("""
            select employee
            from PositionMasterEntity p
            join p.employee employee
            left join p.designation designation
            where p.status = :activeStatus
              and p.positionStatus = :filledStatus
              and upper(trim(coalesce(employee.status, ''))) = :employeeStatus
              and (
                   upper(trim(coalesce(p.positionName, ''))) in :managerNames
                or upper(trim(coalesce(p.positionName, ''))) like :managerNamePattern
                or upper(trim(coalesce(designation.designationName, ''))) in :managerNames
                or upper(trim(coalesce(designation.designationName, ''))) like :managerNamePattern
              )
            order by lower(employee.fullName), employee.employeeId
            """)
    List<EmployeeEntity> findFilledActiveEmployeesByManagerNames(
            @Param("managerNames") Collection<String> managerNames,
            @Param("managerNamePattern") String managerNamePattern,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("filledStatus") PositionStatus filledStatus,
            @Param("employeeStatus") String employeeStatus);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    Optional<PositionMasterEntity> findFirstByProject_ProjectIdAndPositionNameIgnoreCase(
            Long projectId,
            String positionName);

    @Query("select t.teamId as teamId, t.teamName as teamName, t.teamType as teamType, "
            + "count(p.positionId) as totalPositions, "
            + "sum(case when p.positionStatus = :filledStatus then 1 else 0 end) as filledPositions, "
            + "sum(case when p.positionStatus = :vacantStatus then 1 else 0 end) as vacantPositions "
            + "from TeamMasterEntity t "
            + "left join PositionMasterEntity p on p.team.teamId = t.teamId "
            + "and p.status = :activeStatus "
            + "and (:projectId is null or p.project.projectId = :projectId or p.project is null) "
            + "where (:projectId is null "
            + "or t.project.projectId = :projectId "
            + "or exists (select 1 from ProjectMst selectedProject "
            + "where selectedProject.projectId = :projectId "
            + "and selectedProject.cell.cellId = t.cell.cellId)) "
            + "and t.status = :activeStatus "
            + "group by t.teamId, t.teamName, t.teamType, t.displayOrder "
            + "order by t.displayOrder asc, t.teamName asc")
    List<TeamStrengthProjection> getTeamStrength(
            @Param("projectId") Long projectId,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("filledStatus") PositionStatus filledStatus,
            @Param("vacantStatus") PositionStatus vacantStatus);

    @Query("select t.teamId as teamId, t.teamName as teamName, t.teamType as teamType, "
            + "count(p.positionId) as totalPositions, "
            + "sum(case when p.positionStatus = :filledStatus then 1 else 0 end) as filledPositions, "
            + "sum(case when p.positionStatus = :vacantStatus then 1 else 0 end) as vacantPositions "
            + "from TeamMasterEntity t "
            + "left join PositionMasterEntity p on p.team.teamId = t.teamId "
            + "and p.status = :activeStatus "
            + "where t.cell.cellId = :cellId "
            + "and t.status = :activeStatus "
            + "group by t.teamId, t.teamName, t.teamType, t.displayOrder "
            + "order by t.displayOrder asc, t.teamName asc")
    List<TeamStrengthProjection> getTeamStrengthByCell(
            @Param("cellId") Long cellId,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("filledStatus") PositionStatus filledStatus,
            @Param("vacantStatus") PositionStatus vacantStatus);

    @EntityGraph(attributePaths = { "project", "cell", "team", "designation", "employee", "resourceLevel", "reportingPosition" })
    @Query("select p from PositionMasterEntity p "
            + "left join p.project project "
            + "join p.cell cell "
            + "left join p.team team "
            + "left join p.resourceLevel level "
            + "left join p.employee employee "
            + "where p.status = :activeStatus "
            + "and (:projectId is null "
            + "or project.projectId = :projectId "
            + "or (project is null and exists (select 1 from ProjectMst selectedProject "
            + "where selectedProject.projectId = :projectId "
            + "and selectedProject.cell.cellId = cell.cellId))) "
            + "and (:cellId is null or cell.cellId = :cellId) "
            + "and (:search is null "
            + "or lower(p.positionName) like :search "
            + "or lower(p.designation.designationName) like :search "
            + "or lower(coalesce(employee.fullName, '')) like :search "
            + "or lower(coalesce(employee.employeeCode, '')) like :search "
            + "or lower(coalesce(team.teamName, '')) like :search "
            + "or lower(cell.cellName) like :search "
            + "or lower(coalesce(level.levelCode, '')) like :search "
            + "or lower(coalesce(level.levelName, '')) like :search "
            + "or lower(coalesce(project.projectName, '')) like :search "
            + "or lower(coalesce(project.projectCode, '')) like :search) "
            + "order by cell.cellName asc, coalesce(team.teamName, '') asc, p.displayOrder asc, p.positionId asc")
    List<PositionMasterEntity> searchHierarchyPositions(
            @Param("projectId") Long projectId,
            @Param("cellId") Long cellId,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("search") String search,
            Pageable pageable);
}
