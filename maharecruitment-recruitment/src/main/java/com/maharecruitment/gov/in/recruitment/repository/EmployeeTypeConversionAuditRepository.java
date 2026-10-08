package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeTypeConversionAuditEntity;

@Repository
public interface EmployeeTypeConversionAuditRepository
        extends JpaRepository<EmployeeTypeConversionAuditEntity, Long> {

    List<EmployeeTypeConversionAuditEntity> findTop20ByOrderByOccurredAtDescAuditIdDesc();
}
