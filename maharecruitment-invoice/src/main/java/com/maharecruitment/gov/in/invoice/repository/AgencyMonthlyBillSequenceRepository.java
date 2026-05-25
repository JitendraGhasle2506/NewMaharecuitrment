package com.maharecruitment.gov.in.invoice.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillSequenceEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface AgencyMonthlyBillSequenceRepository extends JpaRepository<AgencyMonthlyBillSequenceEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sequence
            from AgencyMonthlyBillSequenceEntity sequence
            where sequence.sequenceDate = :sequenceDate
            """)
    Optional<AgencyMonthlyBillSequenceEntity> findForUpdate(@Param("sequenceDate") LocalDate sequenceDate);
}
