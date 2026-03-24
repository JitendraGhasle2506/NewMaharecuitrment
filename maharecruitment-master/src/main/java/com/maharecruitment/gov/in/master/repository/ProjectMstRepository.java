package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.ProjectScopeType;

@Repository
public interface ProjectMstRepository extends JpaRepository<ProjectMst, Long> {

    Optional<ProjectMst> findFirstByApplicationId(Long applicationId);

    List<ProjectMst> findByProjectScopeTypeOrderByProjectNameAsc(ProjectScopeType projectScopeType);

    Optional<ProjectMst> findByProjectIdAndProjectScopeType(Long projectId, ProjectScopeType projectScopeType);

    Optional<ProjectMst> findFirstByProjectNameIgnoreCaseAndDepartmentRegistrationId(
            String projectName,
            Long departmentRegistrationId);

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
