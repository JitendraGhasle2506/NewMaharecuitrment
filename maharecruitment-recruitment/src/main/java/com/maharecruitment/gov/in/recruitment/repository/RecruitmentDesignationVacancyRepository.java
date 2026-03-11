package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;

@Repository
public interface RecruitmentDesignationVacancyRepository
        extends JpaRepository<RecruitmentDesignationVacancyEntity, Long> {

    Optional<RecruitmentDesignationVacancyEntity>
            findByRecruitmentDesignationVacancyIdAndNotificationRecruitmentNotificationId(
                    Long recruitmentDesignationVacancyId,
                    Long recruitmentNotificationId);
}
