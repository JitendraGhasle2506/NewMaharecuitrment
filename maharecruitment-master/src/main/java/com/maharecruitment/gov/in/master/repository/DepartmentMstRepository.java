package com.maharecruitment.gov.in.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.DepartmentMst;

@Repository
public interface DepartmentMstRepository extends JpaRepository<DepartmentMst, Long> {

    @EntityGraph(attributePaths = "subDepartments")
    @Query("""
            SELECT d
            FROM DepartmentMst d
            WHERE d.departmentId = :departmentId
            """)
    Optional<DepartmentMst> findDetailedByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT COUNT(d) > 0
            FROM DepartmentMst d
            WHERE LOWER(d.departmentName) = LOWER(:departmentName)
              AND (:excludeId IS NULL OR d.departmentId <> :excludeId)
            """)
    boolean existsByDepartmentNameExcludingId(
            @Param("departmentName") String departmentName,
            @Param("excludeId") Long excludeId);
}
