package com.maharecruitment.gov.in.master.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.ManpowerDesignationRate;

@Repository
public interface ManpowerDesignationRateRepository extends JpaRepository<ManpowerDesignationRate, Long> {

    Page<ManpowerDesignationRate> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    Page<ManpowerDesignationRate> findByDesignationIdAndActiveFlagIgnoreCase(Long designationId, String activeFlag,
            Pageable pageable);

    Page<ManpowerDesignationRate> findByDesignationId(Long designationId, Pageable pageable);

    Optional<ManpowerDesignationRate> findByRateIdAndActiveFlagIgnoreCase(Long rateId, String activeFlag);

    @Query("""
            SELECT COUNT(r) > 0
            FROM ManpowerDesignationRate r
            WHERE r.designationId = :designationId
              AND LOWER(r.levelCode) = LOWER(:levelCode)
              AND r.effectiveFrom = :effectiveFrom
              AND (:excludeId IS NULL OR r.rateId <> :excludeId)
            """)
    boolean existsDuplicateExcludingId(
            @Param("designationId") Long designationId,
            @Param("levelCode") String levelCode,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("excludeId") Long excludeId);
}
