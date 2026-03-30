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

    Optional<RecruitmentNotificationEntity> findByDepartmentProjectApplicationId(Long departmentProjectApplicationId);

    Optional<RecruitmentNotificationEntity> findByInternalVacancyOpeningInternalVacancyOpeningId(
            Long internalVacancyOpeningId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification "
            + "from RecruitmentNotificationEntity notification "
            + "where notification.recruitmentNotificationId = :recruitmentNotificationId")
    Optional<RecruitmentNotificationEntity> findByIdForUpdate(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    Page<RecruitmentNotificationEntity> findByStatusIn(
            Collection<RecruitmentNotificationStatus> statuses,
            Pageable pageable);

    @Query("select notification "
            + "from RecruitmentNotificationEntity notification "
            + "left join fetch notification.designationVacancies vacancy "
            + "left join fetch vacancy.designationMst "
            + "left join fetch notification.projectMst "
            + "where notification.recruitmentNotificationId = :recruitmentNotificationId")
    Optional<RecruitmentNotificationEntity> findWithVacanciesById(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select notification "
            + "from RecruitmentNotificationEntity notification "
            + "join fetch notification.projectMst project "
            + "where notification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and notification.departmentRegistrationId = :departmentRegistrationId")
    Optional<RecruitmentNotificationEntity> findForDepartmentReview(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select notification "
            + "from RecruitmentNotificationEntity notification "
            + "join fetch notification.projectMst project "
            + "join notification.internalVacancyOpening opening "
            + "join opening.interviewAuthorities authority "
            + "where upper(notification.requestId) = upper(:requestId) "
            + "and notification.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and authority.user.id = :userId")
    Optional<RecruitmentNotificationEntity> findInternalVacancyForInterviewAuthorityReview(
            @Param("requestId") String requestId,
            @Param("userId") Long userId);
}
