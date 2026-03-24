package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentRequestSequenceEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface RecruitmentRequestSequenceRepository extends JpaRepository<RecruitmentRequestSequenceEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence "
            + "from RecruitmentRequestSequenceEntity sequence "
            + "where sequence.sequenceDate = :sequenceDate "
            + "and sequence.requestTypeCode = :requestTypeCode")
    Optional<RecruitmentRequestSequenceEntity> findForUpdate(
            @Param("sequenceDate") LocalDate sequenceDate,
            @Param("requestTypeCode") String requestTypeCode);
}
