package com.maharecruitment.gov.in.invoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceSequenceEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface DepartmentTaxInvoiceSequenceRepository
        extends JpaRepository<DepartmentTaxInvoiceSequenceEntity, Long> {

    @Modifying
    @Query(value = "insert into department_tax_invoice_sequence (financial_year_code, last_sequence) "
            + "values (:financialYearCode, 0) on conflict (financial_year_code) do nothing", nativeQuery = true)
    void initializeIfAbsent(@Param("financialYearCode") String financialYearCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from DepartmentTaxInvoiceSequenceEntity sequence "
            + "where sequence.financialYearCode = :financialYearCode")
    Optional<DepartmentTaxInvoiceSequenceEntity> findForUpdate(
            @Param("financialYearCode") String financialYearCode);
}
