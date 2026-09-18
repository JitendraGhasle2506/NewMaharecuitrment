package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;

@Repository
public interface EmployeeProjectMappingRepository extends JpaRepository<EmployeeProjectMappingEntity, Long> {

    @EntityGraph(attributePaths = { "employee", "project" })
    Optional<EmployeeProjectMappingEntity> findByEmployeeEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = { "employee", "project" })
    List<EmployeeProjectMappingEntity> findByEmployeeEmployeeIdIn(Collection<Long> employeeIds);

    @Query("""
            select mapping
            from EmployeeProjectMappingEntity mapping
            join fetch mapping.employee employee
            join fetch mapping.project project
            left join fetch employee.departmentRegistration departmentRegistration
            left join fetch employee.department department
            left join fetch employee.subDepartment subDepartment
            left join fetch employee.designation designation
            where upper(trim(coalesce(employee.status, ''))) = 'ACTIVE'
              and trim(coalesce(employee.employeeCode, '')) <> ''
              and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING'
              and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'
              and upper(trim(coalesce(project.activeFlag, ''))) = 'Y'
              and project.departmentId = :departmentId
              and (:subDepartmentId is null or project.subDepartmentId = :subDepartmentId)
              and (:projectId is null or project.projectId = :projectId)
            order by lower(employee.fullName), employee.employeeId
            """)
    List<EmployeeProjectMappingEntity> findCurrentProjectEmployeesForTaxInvoice(
            @Param("departmentId") Long departmentId,
            @Param("subDepartmentId") Long subDepartmentId,
            @Param("projectId") Long projectId);
}
