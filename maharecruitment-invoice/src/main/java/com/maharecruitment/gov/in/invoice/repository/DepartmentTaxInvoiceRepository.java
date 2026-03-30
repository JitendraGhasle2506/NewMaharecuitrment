package com.maharecruitment.gov.in.invoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;

@Repository
public interface DepartmentTaxInvoiceRepository extends JpaRepository<DepartmentTaxInvoiceEntity, Long> {

    Optional<DepartmentTaxInvoiceEntity> findByRequestIdIgnoreCase(String requestId);

    Optional<DepartmentTaxInvoiceEntity> findByDepartmentProjectApplicationId(Long departmentProjectApplicationId);

    boolean existsByRequestIdIgnoreCase(String requestId);

    boolean existsByDepartmentProjectApplicationId(Long departmentProjectApplicationId);
}
