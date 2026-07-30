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

import com.maharecruitment.gov.in.recruitment.entity.organization.OrganizationRecordStatus;
import com.maharecruitment.gov.in.recruitment.entity.organization.TeamMasterEntity;

@Repository
public interface OrganizationTeamRepository extends JpaRepository<TeamMasterEntity, Long> {

    @Override
    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    Optional<TeamMasterEntity> findById(Long teamId);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    @Query("select t from TeamMasterEntity t "
            + "left join t.project project "
            + "join t.cell cell "
            + "where (:projectId is null "
            + "or project.projectId = :projectId "
            + "or exists (select 1 from ProjectMst selectedProject "
            + "where selectedProject.projectId = :projectId "
            + "and selectedProject.cell.cellId = cell.cellId)) "
            + "and (:cellId is null or cell.cellId = :cellId) "
            + "and (:includeInactive = true or t.status = :activeStatus) "
            + "and (:search is null or lower(t.teamName) like :search "
            + "or lower(coalesce(project.projectName, '')) like :search "
            + "or lower(coalesce(project.projectCode, '')) like :search "
            + "or lower(cell.cellName) like :search) "
            + "order by cell.cellName asc, t.displayOrder asc, t.teamName asc")
    Page<TeamMasterEntity> searchTeams(
            @Param("projectId") Long projectId,
            @Param("cellId") Long cellId,
            @Param("includeInactive") boolean includeInactive,
            @Param("activeStatus") OrganizationRecordStatus activeStatus,
            @Param("search") String search,
            Pageable pageable);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    List<TeamMasterEntity> findByProject_ProjectIdOrderByDisplayOrderAscTeamNameAsc(Long projectId);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    List<TeamMasterEntity> findByProject_ProjectIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
            Long projectId,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    List<TeamMasterEntity> findByCell_CellIdAndStatusOrderByDisplayOrderAscTeamNameAsc(
            Long cellId,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    List<TeamMasterEntity> findByProject_ProjectIdInAndStatusOrderByDisplayOrderAscTeamNameAsc(
            List<Long> projectIds,
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    List<TeamMasterEntity> findByStatusOrderByCell_CellNameAscDisplayOrderAscTeamNameAsc(
            OrganizationRecordStatus status);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    Optional<TeamMasterEntity> findFirstByProject_ProjectIdAndTeamNameIgnoreCase(Long projectId, String teamName);

    @EntityGraph(attributePaths = { "project", "cell", "cell.wing", "parentTeam" })
    Optional<TeamMasterEntity> findFirstByCell_CellIdAndTeamNameIgnoreCase(Long cellId, String teamName);

    boolean existsByParentTeam_TeamIdAndStatus(Long parentTeamId, OrganizationRecordStatus status);

    @Query("select count(t) > 0 from TeamMasterEntity t "
            + "where t.cell.cellId = :cellId "
            + "and lower(t.teamName) = lower(:teamName) "
            + "and (:excludeId is null or t.teamId <> :excludeId)")
    boolean existsDuplicateTeam(
            @Param("cellId") Long cellId,
            @Param("teamName") String teamName,
            @Param("excludeId") Long excludeId);
}
