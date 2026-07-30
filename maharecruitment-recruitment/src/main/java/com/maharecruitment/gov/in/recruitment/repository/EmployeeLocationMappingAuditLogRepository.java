package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingAuditLogEntity;

@Repository
public interface EmployeeLocationMappingAuditLogRepository
        extends JpaRepository<EmployeeLocationMappingAuditLogEntity, Long> {

    @EntityGraph(attributePaths = "employee")
    List<EmployeeLocationMappingAuditLogEntity> findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(
            Long employeeId);
}
