package com.maharecruitment.gov.in.department.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentAdvancePaymentEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;

@Repository
public interface DepartmentAdvancePaymentRepository extends JpaRepository<DepartmentAdvancePaymentEntity, Long> {

    List<DepartmentAdvancePaymentEntity> findByDepartmentRegistrationIdOrderByIdDesc(Long departmentRegistrationId);

    List<DepartmentAdvancePaymentEntity> findByApplicationStatusInOrderByIdDesc(
            java.util.Collection<DepartmentApplicationStatus> statuses);

    Optional<DepartmentAdvancePaymentEntity> findByReceiptNumber(String receiptNumber);

    Optional<DepartmentAdvancePaymentEntity> findByApplication(DepartmentProjectApplicationEntity application);

    List<DepartmentAdvancePaymentEntity> findByApplicationAndApplicationStatusNotIn(
            DepartmentProjectApplicationEntity application,
            java.util.Collection<DepartmentApplicationStatus> statuses);

    boolean existsByApplication(DepartmentProjectApplicationEntity application);

    @org.springframework.data.jpa.repository.Query("SELECT p.application.departmentProjectApplicationId FROM DepartmentAdvancePaymentEntity p WHERE p.departmentRegistrationId = :regId")
    List<Long> findApplicationIdsByDepartmentRegistrationId(
            @org.springframework.data.repository.query.Param("regId") Long regId);

    boolean existsByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumberAndIdNot(String receiptNumber, Long id);
}
