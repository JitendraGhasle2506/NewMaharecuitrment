package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface RecruitmentDesignationVacancyRepository
        extends JpaRepository<RecruitmentDesignationVacancyEntity, Long> {

    Optional<RecruitmentDesignationVacancyEntity>
            findByRecruitmentDesignationVacancyIdAndNotificationRecruitmentNotificationId(
                    Long recruitmentDesignationVacancyId,
                    Long recruitmentNotificationId);

    @Query("select vacancy.notification.recruitmentNotificationId "
            + "from RecruitmentDesignationVacancyEntity vacancy "
            + "where vacancy.notification.recruitmentNotificationId in :notificationIds "
            + "group by vacancy.notification.recruitmentNotificationId "
            + "having count(vacancy.recruitmentDesignationVacancyId) > 0 "
            + "and sum(case when coalesce(vacancy.fillPost, 0) < coalesce(vacancy.numberOfVacancy, 0) "
            + "then 1 else 0 end) = 0")
    List<Long> findFullyFilledNotificationIds(@Param("notificationIds") Collection<Long> notificationIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vacancy "
            + "from RecruitmentDesignationVacancyEntity vacancy "
            + "where vacancy.recruitmentDesignationVacancyId = :recruitmentDesignationVacancyId "
            + "and vacancy.notification.recruitmentNotificationId = :recruitmentNotificationId")
    Optional<RecruitmentDesignationVacancyEntity> findByIdForFinalDecisionUpdate(
            @Param("recruitmentDesignationVacancyId") Long recruitmentDesignationVacancyId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("SELECT COALESCE(SUM(v.numberOfVacancy - v.fillPost), 0) FROM RecruitmentDesignationVacancyEntity v")
    long countTotalOpenPositions();
}
