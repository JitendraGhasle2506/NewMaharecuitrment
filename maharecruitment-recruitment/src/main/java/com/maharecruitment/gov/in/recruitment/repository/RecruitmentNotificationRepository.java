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

    @Query("select notification "
            + "from RecruitmentNotificationEntity notification "
            + "join fetch notification.projectMst project "
            + "join fetch notification.internalVacancyOpening opening "
            + "where upper(notification.requestId) = upper(:requestId) "
            + "and upper(opening.createdByEmail) = upper(:actorEmail)")
    Optional<RecruitmentNotificationEntity> findInternalVacancyForOwnerByRequestId(
            @Param("requestId") String requestId,
            @Param("actorEmail") String actorEmail);

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
            + "join fetch notification.internalVacancyOpening opening "
            + "where upper(notification.requestId) = upper(:requestId) "
            + "and notification.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and (upper(opening.createdByEmail) = upper(:actorEmail) "
            + "   or exists (select 1 from opening.interviewAuthorities auth where auth.user.id = :userId) "
            + "   or exists (select 1 from opening.interviewEmployees emp where emp.employee.employeeId = :employeeId))")
    Optional<RecruitmentNotificationEntity> findAccessibleInternalVacancyForCandidateReview(
            @Param("requestId") String requestId,
            @Param("actorEmail") String actorEmail,
            @Param("userId") Long userId,
            @Param("employeeId") Long employeeId);
}
