package com.maharecruitment.gov.in.invoice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillEntity;

@Repository
public interface AgencyMonthlyBillRepository extends JpaRepository<AgencyMonthlyBillEntity, Long> {

    Page<AgencyMonthlyBillEntity> findByActiveTrue(Pageable pageable);

    Optional<AgencyMonthlyBillEntity> findByAgencyIdAndBillYearAndBillMonthAndEmployeeTypeAndActiveTrue(
            Long agencyId,
            Integer billYear,
            Integer billMonth,
            String employeeType);

    Optional<AgencyMonthlyBillEntity> findByAgencyIdAndBillYearAndBillMonthAndEmployeeType(
            Long agencyId,
            Integer billYear,
            Integer billMonth,
            String employeeType);

    @EntityGraph(attributePaths = "lineItems")
    Optional<AgencyMonthlyBillEntity> findDetailedByAgencyMonthlyBillIdAndActiveTrue(Long agencyMonthlyBillId);

    @EntityGraph(attributePaths = "lineItems")
    Optional<AgencyMonthlyBillEntity> findByBillNumberIgnoreCase(String billNumber);
}
