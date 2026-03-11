package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;

@Repository
public interface RecruitmentNotificationRepository extends JpaRepository<RecruitmentNotificationEntity, Long> {

    boolean existsByRequestIdIgnoreCase(String requestId);

    Optional<RecruitmentNotificationEntity> findByRequestIdIgnoreCase(String requestId);
}
