package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface RecruitmentNotificationRepository extends JpaRepository<RecruitmentNotificationEntity, Long> {

    boolean existsByRequestIdIgnoreCase(String requestId);

    Optional<RecruitmentNotificationEntity> findByRequestIdIgnoreCase(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select notification
            from RecruitmentNotificationEntity notification
            where notification.recruitmentNotificationId = :recruitmentNotificationId
            """)
    Optional<RecruitmentNotificationEntity> findByIdForUpdate(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    Page<RecruitmentNotificationEntity> findByStatusIn(
            Collection<RecruitmentNotificationStatus> statuses,
            Pageable pageable);
}
