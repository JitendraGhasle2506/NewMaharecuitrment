package com.maharecruitment.gov.in.master.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.CommissionCode;
import com.maharecruitment.gov.in.master.entity.CommissionRateMaster;

@Repository
public interface CommissionRateMasterRepository extends JpaRepository<CommissionRateMaster, Long> {

    Page<CommissionRateMaster> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    Page<CommissionRateMaster> findByCommissionCode(CommissionCode commissionCode, Pageable pageable);

    Page<CommissionRateMaster> findByCommissionCodeAndActiveFlagIgnoreCase(
            CommissionCode commissionCode,
            String activeFlag,
            Pageable pageable);

    Optional<CommissionRateMaster> findByCommissionRateIdAndActiveFlagIgnoreCase(
            Long commissionRateId,
            String activeFlag);

    boolean existsByCommissionCodeAndEffectiveDate(CommissionCode commissionCode, LocalDate effectiveDate);

    boolean existsByCommissionCodeAndEffectiveDateAndCommissionRateIdNot(
            CommissionCode commissionCode,
            LocalDate effectiveDate,
            Long commissionRateId);

    @Query("""
            SELECT r
            FROM CommissionRateMaster r
            WHERE r.commissionCode = :commissionCode
              AND r.activeFlag = 'Y'
              AND r.effectiveDate <= :date
            ORDER BY r.effectiveDate DESC
            """)
    List<CommissionRateMaster> findApplicableActiveRates(
            @Param("commissionCode") CommissionCode commissionCode,
            @Param("date") LocalDate date);
}
