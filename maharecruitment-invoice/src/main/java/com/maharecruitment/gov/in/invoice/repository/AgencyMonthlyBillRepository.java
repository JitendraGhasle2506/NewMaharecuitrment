package com.maharecruitment.gov.in.invoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<AgencyMonthlyBillEntity> findFirstByAgencyIdAndBillYearAndBillMonthOrderByAgencyMonthlyBillIdDesc(
            Long agencyId,
            Integer billYear,
            Integer billMonth);

    @Query("""
            select distinct line.employeeId
            from AgencyMonthlyBillEntity bill
            join bill.lineItems line
            where bill.active = true
              and bill.agencyId = :agencyId
              and bill.billYear = :billYear
              and bill.billMonth = :billMonth
              and (:excludedBillId is null or bill.agencyMonthlyBillId <> :excludedBillId)
            """)
    List<Long> findBilledEmployeeIdsForPeriod(
            @Param("agencyId") Long agencyId,
            @Param("billYear") Integer billYear,
            @Param("billMonth") Integer billMonth,
            @Param("excludedBillId") Long excludedBillId);

    @EntityGraph(attributePaths = "lineItems")
    Optional<AgencyMonthlyBillEntity> findDetailedByAgencyMonthlyBillIdAndActiveTrue(Long agencyMonthlyBillId);

    @EntityGraph(attributePaths = "lineItems")
    Optional<AgencyMonthlyBillEntity> findByBillNumberIgnoreCase(String billNumber);
}
