package com.maharecruitment.gov.in.recruitment.repository;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vacancy "
            + "from RecruitmentDesignationVacancyEntity vacancy "
            + "where vacancy.recruitmentDesignationVacancyId = :recruitmentDesignationVacancyId "
            + "and vacancy.notification.recruitmentNotificationId = :recruitmentNotificationId")
    Optional<RecruitmentDesignationVacancyEntity> findByIdForFinalDecisionUpdate(
            @Param("recruitmentDesignationVacancyId") Long recruitmentDesignationVacancyId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);
}
