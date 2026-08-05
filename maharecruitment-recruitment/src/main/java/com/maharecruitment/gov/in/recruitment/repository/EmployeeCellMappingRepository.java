package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingEntity;

@Repository
public interface EmployeeCellMappingRepository extends JpaRepository<EmployeeCellMappingEntity, Long> {

    @EntityGraph(attributePaths = { "employee", "cell", "cell.wing" })
    Optional<EmployeeCellMappingEntity> findByEmployeeEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = { "employee", "cell", "cell.wing" })
    List<EmployeeCellMappingEntity> findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAsc(
            Collection<Long> employeeIds);

    @Query("""
            select cell.cellId as cellId,
                   count(distinct employee.employeeId) as employeeCount
            from EmployeeCellMappingEntity mapping
            join mapping.employee employee
            join mapping.cell cell
            join cell.wing wing
            where upper(trim(coalesce(employee.status, ''))) = :employeeStatus
              and trim(coalesce(employee.employeeCode, '')) <> ''
              and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING'
              and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'
              and upper(coalesce(cell.activeFlag, 'N')) = :activeFlag
              and upper(coalesce(wing.activeFlag, 'N')) = :activeFlag
            group by cell.cellId
            """)
    List<EmployeeCellCountProjection> summarizeActiveEmployeesByCell(
            @Param("activeFlag") String activeFlag,
            @Param("employeeStatus") String employeeStatus);

    @Query("""
            select cell.cellId as cellId,
                   count(distinct employee.employeeId) as employeeCount
            from EmployeeCellMappingEntity mapping
            join mapping.employee employee
            join mapping.cell cell
            join cell.wing wing
            where wing.wingId = :wingId
              and upper(trim(coalesce(employee.status, ''))) = :employeeStatus
              and trim(coalesce(employee.employeeCode, '')) <> ''
              and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING'
              and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'
              and upper(coalesce(cell.activeFlag, 'N')) = :activeFlag
              and upper(coalesce(wing.activeFlag, 'N')) = :activeFlag
            group by cell.cellId
            """)
    List<EmployeeCellCountProjection> summarizeActiveEmployeesByCellAndWingId(
            @Param("wingId") Long wingId,
            @Param("activeFlag") String activeFlag,
            @Param("employeeStatus") String employeeStatus);

    @Query("""
            select distinct employee.employeeId
            from EmployeeCellMappingEntity mapping
            join mapping.employee employee
            where mapping.cell.cellId in :cellIds
              and (employee.user is null or employee.user.id <> :authorityUserId)
              and not exists (
                    select reporting.mappingId
                    from EmployeeReportingMappingEntity reporting
                    where reporting.employeeId = employee.employeeId
              )
            order by employee.employeeId
            """)
    List<Long> findEmployeeIdsWithoutExplicitReportingMapping(
            @Param("cellIds") Collection<Long> cellIds,
            @Param("authorityUserId") Long authorityUserId);

    @Query("""
            select cell.cellId as cellId,
                   count(distinct employee.employeeId) as employeeCount
            from EmployeeCellMappingEntity mapping
            join mapping.employee employee
            join mapping.cell cell
            join cell.wing wing
            where upper(trim(coalesce(employee.status, ''))) = :employeeStatus
              and trim(coalesce(employee.employeeCode, '')) <> ''
              and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING'
              and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'
              and upper(coalesce(cell.activeFlag, 'N')) = :activeFlag
              and upper(coalesce(wing.activeFlag, 'N')) = :activeFlag
              and not exists (
                    select reporting.mappingId
                    from EmployeeReportingMappingEntity reporting
                    where reporting.employeeId = employee.employeeId
              )
            group by cell.cellId
            """)
    List<EmployeeCellCountProjection> summarizeActiveEmployeesWithoutExplicitReportingByCell(
            @Param("activeFlag") String activeFlag,
            @Param("employeeStatus") String employeeStatus);
}
