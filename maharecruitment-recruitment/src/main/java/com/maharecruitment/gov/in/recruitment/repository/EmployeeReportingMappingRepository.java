package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;

@Repository
public interface EmployeeReportingMappingRepository extends JpaRepository<EmployeeReportingMappingEntity, Long> {

    List<EmployeeReportingMappingEntity> findByProjectId(Long projectId);

    List<EmployeeReportingMappingEntity> findByManagerEmployeeId(Long managerEmployeeId);

    List<EmployeeReportingMappingEntity> findByHodUserId(Long hodUserId);

    List<EmployeeReportingMappingEntity> findByEmployeeIdIn(Collection<Long> employeeIds);

    EmployeeReportingMappingEntity findByEmployeeId(Long employeeId);

    Optional<EmployeeReportingMappingEntity> findFirstByEmployeeIdOrderByMappingIdDesc(Long employeeId);

    @Query("""
            select distinct mapping.employeeId
            from EmployeeReportingMappingEntity mapping
            where mapping.hodUserId = :authorityUserId
               or (:managerEmployeeId is not null and mapping.managerEmployeeId = :managerEmployeeId)
            order by mapping.employeeId
            """)
    List<Long> findEmployeeIdsByAuthorityIdentity(
            @Param("authorityUserId") Long authorityUserId,
            @Param("managerEmployeeId") Long managerEmployeeId);

    @Query("""
            select mapping.employeeId as employeeId,
                   mapping.hodUserId as hodUserId,
                   mapping.mappingId as mappingId
            from EmployeeReportingMappingEntity mapping
            where mapping.employeeId in :employeeIds
              and mapping.hodUserId is not null
            """)
    List<EmployeeReportingHodProjection> findHodReferencesByEmployeeIdIn(
            @Param("employeeIds") Collection<Long> employeeIds);
}
