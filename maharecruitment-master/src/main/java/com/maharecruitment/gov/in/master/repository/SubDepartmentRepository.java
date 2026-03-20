package com.maharecruitment.gov.in.master.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.SubDepartment;

@Repository
public interface SubDepartmentRepository extends JpaRepository<SubDepartment, Long> {

    Page<SubDepartment> findByDepartmentDepartmentId(Long departmentId, Pageable pageable);

    Optional<SubDepartment> findBySubDeptIdAndDepartmentDepartmentId(Long subDeptId, Long departmentId);

    boolean existsByDepartmentDepartmentId(Long departmentId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM SubDepartment s
            WHERE s.department.departmentId = :departmentId
              AND LOWER(s.subDeptName) = LOWER(:subDeptName)
              AND (:excludeId IS NULL OR s.subDeptId <> :excludeId)
            """)
    boolean existsByDepartmentIdAndSubDeptNameExcludingId(
            @Param("departmentId") Long departmentId,
            @Param("subDeptName") String subDeptName,
            @Param("excludeId") Long excludeId);
}
