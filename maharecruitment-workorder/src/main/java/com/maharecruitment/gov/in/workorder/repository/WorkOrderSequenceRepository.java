package com.maharecruitment.gov.in.workorder.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.workorder.entity.WorkOrderSequenceEntity;

import jakarta.persistence.LockModeType;

public interface WorkOrderSequenceRepository extends JpaRepository<WorkOrderSequenceEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from WorkOrderSequenceEntity sequence where sequence.sequenceKey = :sequenceKey")
    Optional<WorkOrderSequenceEntity> findBySequenceKeyForUpdate(@Param("sequenceKey") String sequenceKey);
}
