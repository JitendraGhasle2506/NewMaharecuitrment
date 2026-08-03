package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;

@Repository
public interface EmployeeLocationMappingRepository extends JpaRepository<EmployeeLocationMappingEntity, Long> {

    @EntityGraph(attributePaths = { "employee", "location" })
    List<EmployeeLocationMappingEntity> findByEmployeeEmployeeIdOrderByLocationLocationNameAsc(Long employeeId);

    @EntityGraph(attributePaths = { "employee", "location" })
    List<EmployeeLocationMappingEntity> findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(
            Long employeeId);

    @EntityGraph(attributePaths = { "employee", "location" })
    List<EmployeeLocationMappingEntity> findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAscPrimaryLocationDescLocationLocationNameAsc(
            Collection<Long> employeeIds);

    @EntityGraph(attributePaths = { "employee", "location" })
    Optional<EmployeeLocationMappingEntity> findByEmployeeEmployeeIdAndPrimaryLocationTrue(Long employeeId);

    boolean existsByEmployeeEmployeeIdAndLocationLocationIdIn(Long employeeId, Collection<Long> locationIds);

    boolean existsByEmployeeEmployeeIdAndPrimaryLocationTrueAndEmployeeLocationMappingIdNot(
            Long employeeId,
            Long employeeLocationMappingId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE EmployeeLocationMappingEntity m SET m.primaryLocation = false WHERE m.employee.employeeId = :employeeId AND m.primaryLocation = true")
    int clearPrimaryForEmployee(@Param("employeeId") Long employeeId);
}
