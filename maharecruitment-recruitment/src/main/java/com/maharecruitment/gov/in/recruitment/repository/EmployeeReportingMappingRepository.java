package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;

@Repository
public interface EmployeeReportingMappingRepository extends JpaRepository<EmployeeReportingMappingEntity, Long> {
    
    List<EmployeeReportingMappingEntity> findByProjectId(Long projectId);
    
    List<EmployeeReportingMappingEntity> findByManagerEmployeeId(Long managerEmployeeId);
    
    List<EmployeeReportingMappingEntity> findByHodUserId(Long hodUserId);
    
    EmployeeReportingMappingEntity findByEmployeeId(Long employeeId);
}
