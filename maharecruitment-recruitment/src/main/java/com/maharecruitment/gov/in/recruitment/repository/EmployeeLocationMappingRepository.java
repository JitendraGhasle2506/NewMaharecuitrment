package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;

@Repository
public interface EmployeeLocationMappingRepository extends JpaRepository<EmployeeLocationMappingEntity, Long> {

    @EntityGraph(attributePaths = { "employee", "location" })
    List<EmployeeLocationMappingEntity> findByEmployeeEmployeeIdOrderByLocationLocationNameAsc(Long employeeId);

    @EntityGraph(attributePaths = { "employee", "location" })
    List<EmployeeLocationMappingEntity> findByEmployeeEmployeeIdInOrderByEmployeeEmployeeIdAscLocationLocationNameAsc(
            Collection<Long> employeeIds);

    boolean existsByEmployeeEmployeeIdAndLocationLocationIdIn(Long employeeId, Collection<Long> locationIds);
}
