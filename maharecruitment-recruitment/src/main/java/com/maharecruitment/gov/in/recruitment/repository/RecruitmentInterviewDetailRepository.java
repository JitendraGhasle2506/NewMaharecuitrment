package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;

@Repository
public interface RecruitmentInterviewDetailRepository extends JpaRepository<RecruitmentInterviewDetailEntity, Long> {

    @Query("""
            select candidate
            from RecruitmentInterviewDetailEntity candidate
            join fetch candidate.designationVacancy vacancy
            left join fetch vacancy.designationMst designation
            where candidate.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId
              and candidate.agency.agencyId = :agencyId
              and candidate.active = true
            order by candidate.recruitmentInterviewDetailId desc
            """)
    List<RecruitmentInterviewDetailEntity> findActiveCandidatesByNotificationAndAgency(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("agencyId") Long agencyId);

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndCandidateEmailIgnoreCase(
            Long recruitmentNotificationId,
            Long agencyId,
            String candidateEmail);

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndCandidateMobile(
            Long recruitmentNotificationId,
            Long agencyId,
            String candidateMobile);

    Optional<RecruitmentInterviewDetailEntity>
            findByRecruitmentInterviewDetailIdAndRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                    Long recruitmentInterviewDetailId,
                    Long recruitmentNotificationId,
                    Long agencyId);
}
