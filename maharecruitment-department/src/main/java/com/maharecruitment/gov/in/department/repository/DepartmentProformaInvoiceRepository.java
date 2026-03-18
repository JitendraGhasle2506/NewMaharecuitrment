package com.maharecruitment.gov.in.department.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentProformaInvoiceEntity;

@Repository
public interface DepartmentProformaInvoiceRepository extends JpaRepository<DepartmentProformaInvoiceEntity, Long> {

    Optional<DepartmentProformaInvoiceEntity> findByPiNumber(String piNumber);

    List<DepartmentProformaInvoiceEntity> findByApplication_DepartmentProjectApplicationIdOrderByDepartmentProformaInvoiceIdDesc(Long applicationId);

    @Query("SELECT MAX(p.piNumber) FROM DepartmentProformaInvoiceEntity p WHERE p.piNumber LIKE :prefix")
    String findMaxPiNumberByPrefix(String prefix);
}
