package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeCellMappingAuditLogEntity;

@Repository
public interface EmployeeCellMappingAuditLogRepository
        extends JpaRepository<EmployeeCellMappingAuditLogEntity, Long> {

    @EntityGraph(attributePaths = { "employee" })
    List<EmployeeCellMappingAuditLogEntity> findTop10ByEmployeeEmployeeIdOrderByOccurredAtDescAuditIdDesc(
            Long employeeId);
}
