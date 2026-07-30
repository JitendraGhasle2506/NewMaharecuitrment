package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;

@Repository
public interface ProjectMstRepository extends JpaRepository<ProjectMst, Long> {

    @Override
    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    List<ProjectMst> findAll();

    @Override
    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    Page<ProjectMst> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    Page<ProjectMst> findByCell_CellId(Long cellId, Pageable pageable);

    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    Page<ProjectMst> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    Page<ProjectMst> findByCell_CellIdAndActiveFlagIgnoreCase(Long cellId, String activeFlag, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    Optional<ProjectMst> findById(Long projectId);

    Optional<ProjectMst> findFirstByApplicationId(Long applicationId);

    List<ProjectMst> findByProjectScopeTypeOrderByProjectNameAsc(ProjectScopeType projectScopeType);

    List<ProjectMst> findByProjectScopeTypeAndActiveFlagIgnoreCaseOrderByProjectNameAsc(
            ProjectScopeType projectScopeType,
            String activeFlag);

    @EntityGraph(attributePaths = { "cell", "cell.wing" })
    List<ProjectMst> findByActiveFlagIgnoreCaseOrderByProjectNameAsc(String activeFlag);

    long countByProjectScopeType(ProjectScopeType projectScopeType);

    @Query("""
            select cell.cellId as cellId,
                   count(project.projectId) as projectCount
            from ProjectMst project
            join project.cell cell
            group by cell.cellId
            """)
    List<ProjectCellCountProjection> summarizeProjectCountsByCell();

    @Query("""
            select cell.cellId as cellId,
                   count(project.projectId) as projectCount
            from ProjectMst project
            join project.cell cell
            join cell.wing wing
            where wing.wingId = :wingId
            group by cell.cellId
            """)
    List<ProjectCellCountProjection> summarizeProjectCountsByCellAndWingId(@Param("wingId") Long wingId);

    @Query("""
            select
                case
                    when trim(coalesce(cell.cellName, '')) = '' then :unassignedCell
                    else trim(cell.cellName)
                end as cellName,
                count(project.projectId) as totalProjects,
                coalesce(sum(case when project.projectScopeType = :internalScope then 1 else 0 end), 0) as internalProjects,
                coalesce(sum(case when project.projectScopeType = :externalScope then 1 else 0 end), 0) as externalProjects
            from ProjectMst project
            left join project.cell cell
            group by
                case
                    when trim(coalesce(cell.cellName, '')) = '' then :unassignedCell
                    else trim(cell.cellName)
                end
            """)
    List<ProjectCellSummaryProjection> summarizeProjectsByCell(
            @Param("internalScope") ProjectScopeType internalScope,
            @Param("externalScope") ProjectScopeType externalScope,
            @Param("unassignedCell") String unassignedCell);

    Optional<ProjectMst> findByProjectIdAndProjectScopeType(Long projectId, ProjectScopeType projectScopeType);

    Optional<ProjectMst> findByProjectIdAndProjectScopeTypeAndActiveFlagIgnoreCase(
            Long projectId,
            ProjectScopeType projectScopeType,
            String activeFlag);

    Optional<ProjectMst> findFirstByProjectNameIgnoreCaseAndDepartmentRegistrationId(
            String projectName,
            Long departmentRegistrationId);

    @Query("select count(p) > 0 "
            + "from ProjectMst p "
            + "where lower(p.projectCode) = lower(:projectCode) "
            + "and (:excludeId is null or p.projectId <> :excludeId)")
    boolean existsByProjectCodeExcludingId(
            @Param("projectCode") String projectCode,
            @Param("excludeId") Long excludeId);

    @Query("select count(p) > 0 "
            + "from ProjectMst p "
            + "where lower(p.projectName) = lower(:projectName) "
            + "and ((:departmentRegistrationId is null and p.departmentRegistrationId is null) "
            + "or p.departmentRegistrationId = :departmentRegistrationId) "
            + "and (:excludeId is null or p.projectId <> :excludeId)")
    boolean existsByProjectNameAndDepartmentRegistrationIdExcludingId(
            @Param("projectName") String projectName,
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("excludeId") Long excludeId);
}
