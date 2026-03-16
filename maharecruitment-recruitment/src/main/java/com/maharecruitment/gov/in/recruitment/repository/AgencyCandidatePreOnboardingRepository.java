package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;

@Repository
public interface AgencyCandidatePreOnboardingRepository extends JpaRepository<AgencyCandidatePreOnboardingEntity, Long> {

    @Query("select distinct preOnboarding "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "join fetch preOnboarding.interviewDetail candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch candidate.agency agency "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "left join fetch preOnboarding.previousEmployments previousEmployment "
            + "where candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and candidate.agency.agencyId = :agencyId")
    Optional<AgencyCandidatePreOnboardingEntity> findByInterviewDetailIdAndAgencyIdForForm(
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId,
            @Param("agencyId") Long agencyId);

    @Query("select preOnboarding "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "join fetch preOnboarding.interviewDetail candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch candidate.agency agency "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and candidate.active = true "
            + "and candidate.finalDecisionStatus = 'SELECTED' "
            + "and preOnboarding.onboardedAt is null "
            + "order by preOnboarding.submittedAt desc, preOnboarding.updatedDateTime desc")
    List<AgencyCandidatePreOnboardingEntity> findOnboardingReadyCandidatesByAgency(
            @Param("agencyId") Long agencyId);

    @Query("select preOnboarding "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "join fetch preOnboarding.interviewDetail candidate "
            + "where candidate.recruitmentInterviewDetailId in :interviewDetailIds")
    List<AgencyCandidatePreOnboardingEntity> findByInterviewDetailIds(
            @Param("interviewDetailIds") Collection<Long> interviewDetailIds);

    @Query("select preOnboarding "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "join fetch preOnboarding.interviewDetail candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch candidate.agency agency "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where preOnboarding.submittedAt is not null "
            + "and preOnboarding.hrVerified = false "
            + "order by preOnboarding.submittedAt asc")
    List<AgencyCandidatePreOnboardingEntity> findPendingHROnboarding();

    Optional<AgencyCandidatePreOnboardingEntity> findByInterviewDetailRecruitmentInterviewDetailId(
            Long recruitmentInterviewDetailId);

    long countByInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndOnboardedAtIsNotNull(
            Long recruitmentDesignationVacancyId);
}
