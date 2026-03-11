package com.maharecruitment.gov.in.department.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentAdvancePaymentActivityEntity;

@Repository
public interface DepartmentAdvancePaymentActivityRepository extends JpaRepository<DepartmentAdvancePaymentActivityEntity, Long> {
    
    List<DepartmentAdvancePaymentActivityEntity> findByPaymentIdOrderByActionTimestampDesc(Long paymentId);
}
