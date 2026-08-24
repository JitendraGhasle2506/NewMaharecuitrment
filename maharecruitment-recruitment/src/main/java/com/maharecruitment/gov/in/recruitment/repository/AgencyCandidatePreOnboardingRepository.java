package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;

@Repository
public interface AgencyCandidatePreOnboardingRepository extends JpaRepository<AgencyCandidatePreOnboardingEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select preOnboarding from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where preOnboarding.preOnboardingId = :preOnboardingId")
    Optional<AgencyCandidatePreOnboardingEntity> findByIdForOnboardingUpdate(
            @Param("preOnboardingId") Long preOnboardingId);

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
            + "and preOnboarding.onboardedAt is null "
            + "order by preOnboarding.submittedAt asc")
    List<AgencyCandidatePreOnboardingEntity> findPendingHROnboarding();

    Optional<AgencyCandidatePreOnboardingEntity> findByInterviewDetailRecruitmentInterviewDetailId(
            Long recruitmentInterviewDetailId);

    @Query("select case when count(preOnboarding) > 0 then true else false end "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where (:excludePreOnboardingId is null or preOnboarding.preOnboardingId <> :excludePreOnboardingId) "
            + "and trim(coalesce(preOnboarding.aadhaarNumber, '')) = :aadhaarNumber")
    boolean existsByAadhaarNumberExcludingPreOnboardingId(
            @Param("aadhaarNumber") String aadhaarNumber,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    @Query("select case when count(preOnboarding) > 0 then true else false end "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where (:excludePreOnboardingId is null or preOnboarding.preOnboardingId <> :excludePreOnboardingId) "
            + "and upper(trim(coalesce(preOnboarding.panNumber, ''))) = upper(trim(:panNumber))")
    boolean existsByPanNumberExcludingPreOnboardingId(
            @Param("panNumber") String panNumber,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    @Query("select case when count(preOnboarding) > 0 then true else false end "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where (:excludePreOnboardingId is null or preOnboarding.preOnboardingId <> :excludePreOnboardingId) "
            + "and lower(trim(coalesce(preOnboarding.candidateEmail, ''))) = lower(trim(:email))")
    boolean existsByCandidateEmailExcludingPreOnboardingId(
            @Param("email") String email,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    @Query("select case when count(preOnboarding) > 0 then true else false end "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where (:excludePreOnboardingId is null or preOnboarding.preOnboardingId <> :excludePreOnboardingId) "
            + "and trim(coalesce(preOnboarding.candidateMobile, '')) = :mobile")
    boolean existsByCandidateMobileExcludingPreOnboardingId(
            @Param("mobile") String mobile,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    long countByInterviewDetailAgencyAgencyId(Long agencyId);

    long countByInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndOnboardedAtIsNotNull(
            Long recruitmentDesignationVacancyId);
}
