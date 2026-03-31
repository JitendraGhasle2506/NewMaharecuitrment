package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.repository.projection.AgencySelectedCandidateProjectSummaryProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.DepartmentNotificationCandidateSummaryProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.InternalVacancyCandidateRequestSummaryMetricsProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.InternalVacancyCandidateRequestSummaryProjection;

import jakarta.persistence.LockModeType;

@Repository
public interface RecruitmentInterviewDetailRepository extends JpaRepository<RecruitmentInterviewDetailEntity, Long> {

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and candidate.agency.agencyId = :agencyId "
            + "and candidate.active = true "
            + "order by candidate.recruitmentInterviewDetailId desc")
    List<RecruitmentInterviewDetailEntity> findActiveCandidatesByNotificationAndAgency(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("agencyId") Long agencyId);

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and candidate.active = true "
            + "and candidate.finalDecisionStatus = 'SELECTED' "
            + "and not exists (select preOnboarding.preOnboardingId "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where preOnboarding.interviewDetail = candidate and preOnboarding.onboardedAt is not null) "
            + "order by candidate.finalDecisionAt desc, candidate.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findSelectedCandidatesByAgency(@Param("agencyId") Long agencyId);

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and candidate.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and candidate.active = true "
            + "and candidate.finalDecisionStatus = 'SELECTED' "
            + "and not exists (select preOnboarding.preOnboardingId "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where preOnboarding.interviewDetail = candidate and preOnboarding.onboardedAt is not null) "
            + "order by candidate.finalDecisionAt desc, candidate.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findSelectedCandidatesByAgencyAndNotification(
            @Param("agencyId") Long agencyId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select n.recruitmentNotificationId as recruitmentNotificationId, "
            + "n.requestId as requestId, "
            + "p.projectName as projectName, "
            + "count(c.recruitmentInterviewDetailId) as selectedCandidatesCount, "
            + "max(c.finalDecisionAt) as latestDecisionAt "
            + "from RecruitmentInterviewDetailEntity c "
            + "join c.recruitmentNotification n "
            + "join n.projectMst p "
            + "where c.agency.agencyId = :agencyId "
            + "and c.active = true "
            + "and c.finalDecisionStatus = 'SELECTED' "
            + "and not exists (select preOnboarding.preOnboardingId "
            + "from AgencyCandidatePreOnboardingEntity preOnboarding "
            + "where preOnboarding.interviewDetail = c and preOnboarding.onboardedAt is not null) "
            + "group by n.recruitmentNotificationId, n.requestId, p.projectName "
            + "order by max(c.finalDecisionAt) desc")
    List<AgencySelectedCandidateProjectSummaryProjection> findSelectedCandidateProjectSummariesByAgency(
            @Param("agencyId") Long agencyId);

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch notification.internalVacancyOpening opening "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and candidate.active = true "
            + "and candidate.assessmentSubmitted = true "
            + "and notification.internalVacancyOpening is not null "
            + "and notification.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "order by coalesce(candidate.assessmentSubmittedAt, candidate.createdDateTime) desc, "
            + "candidate.recruitmentInterviewDetailId desc")
    List<RecruitmentInterviewDetailEntity> findInternalAssessmentSubmittedCandidatesByAgency(
            @Param("agencyId") Long agencyId);

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch notification.internalVacancyOpening opening "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and notification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and candidate.active = true "
            + "and candidate.assessmentSubmitted = true "
            + "and notification.internalVacancyOpening is not null "
            + "and notification.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "order by coalesce(candidate.assessmentSubmittedAt, candidate.createdDateTime) desc, "
            + "candidate.recruitmentInterviewDetailId desc")
    List<RecruitmentInterviewDetailEntity> findInternalAssessmentSubmittedCandidatesByAgencyAndNotification(
            @Param("agencyId") Long agencyId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch notification.internalVacancyOpening opening "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and notification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and candidate.active = true "
            + "and candidate.assessmentSubmitted = true "
            + "and notification.internalVacancyOpening is not null "
            + "and notification.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN")
    Optional<RecruitmentInterviewDetailEntity> findInternalAssessmentSubmittedCandidateByAgencyAndNotificationAndId(
            @Param("agencyId") Long agencyId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch notification.internalVacancyOpening opening "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.agency.agencyId = :agencyId "
            + "and notification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and candidate.active = true "
            + "and candidate.assessmentSubmitted = true "
            + "and notification.internalVacancyOpening is not null "
            + "and notification.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN")
    Optional<RecruitmentInterviewDetailEntity> findInternalAssessmentSubmittedCandidateByAgencyAndNotificationAndIdForUpdate(
            @Param("agencyId") Long agencyId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);

    @Query("select n.recruitmentNotificationId as recruitmentNotificationId, "
            + "n.requestId as requestId, "
            + "n.departmentProjectApplicationId as departmentProjectApplicationId, "
            + "p.projectName as projectName, "
            + "count(c.recruitmentInterviewDetailId) as totalCandidates, "
            + "coalesce(sum(case when c.departmentShortlistedAt is null then 1 else 0 end), 0) as pendingCandidates, "
            + "coalesce(sum(case when c.departmentShortlistedAt is not null and c.candidateStatus <> "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT "
            + "and (c.finalDecisionStatus is null or c.finalDecisionStatus <> 'SELECTED') "
            + "then 1 else 0 end), 0) as shortlistedCandidates, "
            + "coalesce(sum(case when c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT "
            + "then 1 else 0 end), 0) as rejectedCandidates, "
            + "coalesce(sum(case when c.assessmentSubmitted = true and c.finalDecisionStatus is null then 1 else 0 end), 0) "
            + "as assessmentPendingFinalDecisionCandidates, "
            + "coalesce(sum(case when c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY "
            + "and c.departmentShortlistedAt is not null "
            + "and c.interviewDateTime is not null "
            + "and c.finalDecisionStatus is null then 1 else 0 end), 0) "
            + "as availableForInterviewScheduleCandidates, "
            + "coalesce(sum(case when c.finalDecisionStatus = 'SELECTED' then 1 else 0 end), 0) "
            + "as selectedCandidates, "
            + "max(c.createdDateTime) as latestSubmittedAt "
            + "from RecruitmentInterviewDetailEntity c "
            + "join c.recruitmentNotification n "
            + "join n.projectMst p "
            + "where n.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true "
            + "group by n.recruitmentNotificationId, n.requestId, n.departmentProjectApplicationId, p.projectName "
            + "order by max(c.createdDateTime) desc")
    List<DepartmentNotificationCandidateSummaryProjection> findDepartmentCandidateSummaries(
            @Param("departmentRegistrationId") Long departmentRegistrationId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where n.recruitmentNotificationId = :recruitmentNotificationId "
            + "and n.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true "
            + "order by c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findCandidatesForDepartmentReview(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where upper(n.requestId) = upper(:requestId) "
            + "and n.internalVacancyOpening is not null "
            + "and c.active = true "
            + "order by case when c.interviewDateTime is null then 1 else 0 end asc, "
            + "c.interviewDateTime desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findActiveCandidatesForInternalVacancyByRequestId(
            @Param("requestId") String requestId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join n.internalVacancyOpening opening "
            + "join opening.interviewAuthorities authority "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where authority.user.id = :userId "
            + "and n.internalVacancyOpening is not null "
            + "and n.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and c.active = true "
            + "order by upper(n.requestId) asc, "
            + "case when c.interviewDateTime is null then 1 else 0 end asc, "
            + "c.interviewDateTime desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findActiveCandidatesForInternalVacanciesByInterviewAuthorityUserId(
            @Param("userId") Long userId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join n.internalVacancyOpening opening "
            + "join opening.interviewAuthorities authority "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where upper(n.requestId) = upper(:requestId) "
            + "and authority.user.id = :userId "
            + "and n.internalVacancyOpening is not null "
            + "and n.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and c.active = true "
            + "order by case when c.interviewDateTime is null then 1 else 0 end asc, "
            + "c.interviewDateTime desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findActiveCandidatesForInternalVacancyByRequestIdAndInterviewAuthorityUserId(
            @Param("requestId") String requestId,
            @Param("userId") Long userId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where n.internalVacancyOpening is not null "
            + "and c.active = true "
            + "order by upper(n.requestId) asc, "
            + "case when c.interviewDateTime is null then 1 else 0 end asc, "
            + "c.interviewDateTime desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findActiveCandidatesForInternalVacancies();

    @Query(
            value = "select upper(n.requestId) as requestId, "
                    + "p.projectName as projectName, "
                    + "count(c.recruitmentInterviewDetailId) as totalCandidates, "
                    + "coalesce(sum(case when c.candidateStatus is null then 1 else 0 end), 0) as pendingReviewCandidates, "
                    + "coalesce(sum(case when c.candidateStatus = "
                    + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT "
                    + "then 1 else 0 end), 0) as shortlistedCandidates, "
                    + "coalesce(sum(case when c.candidateStatus = "
                    + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT "
                    + "then 1 else 0 end), 0) as rejectedCandidates, "
                    + "coalesce(sum(case when c.candidateStatus = "
                    + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY "
                    + "and (c.assessmentSubmitted is null or c.assessmentSubmitted = false) then 1 else 0 end), 0) "
                    + "as interviewScheduledCandidates, "
                    + "coalesce(sum(case when c.assessmentSubmitted = true then 1 else 0 end), 0) "
                    + "as feedbackSubmittedCandidates, "
                    + "max(c.createdDateTime) as latestSubmittedAt "
                    + "from RecruitmentInterviewDetailEntity c "
                    + "join c.recruitmentNotification n "
                    + "join n.projectMst p "
                    + "where n.internalVacancyOpening is not null "
                    + "and c.active = true "
                    + "and (:searchPattern is null "
                    + "or upper(n.requestId) like :searchPattern "
                    + "or upper(p.projectName) like :searchPattern) "
                    + "group by n.recruitmentNotificationId, n.requestId, p.projectName "
                    + "order by max(c.createdDateTime) desc",
            countQuery = "select count(distinct n.recruitmentNotificationId) "
                    + "from RecruitmentInterviewDetailEntity c "
                    + "join c.recruitmentNotification n "
                    + "join n.projectMst p "
                    + "where n.internalVacancyOpening is not null "
                    + "and c.active = true "
                    + "and (:searchPattern is null "
                    + "or upper(n.requestId) like :searchPattern "
                    + "or upper(p.projectName) like :searchPattern)")
    Page<InternalVacancyCandidateRequestSummaryProjection> findInternalVacancyCandidateRequestSummaryPage(
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select count(distinct n.recruitmentNotificationId) as requestCount, "
            + "count(c.recruitmentInterviewDetailId) as totalCandidates, "
            + "coalesce(sum(case when c.candidateStatus is null then 1 else 0 end), 0) as pendingReviewCandidates, "
            + "coalesce(sum(case when c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.SHORTLISTED_BY_DEPARTMENT "
            + "then 1 else 0 end), 0) as shortlistedCandidates, "
            + "coalesce(sum(case when c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.REJECTED_BY_DEPARTMENT "
            + "then 1 else 0 end), 0) as rejectedCandidates, "
            + "coalesce(sum(case when c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY "
            + "and (c.assessmentSubmitted is null or c.assessmentSubmitted = false) then 1 else 0 end), 0) "
            + "as interviewScheduledCandidates, "
            + "coalesce(sum(case when c.assessmentSubmitted = true then 1 else 0 end), 0) "
            + "as feedbackSubmittedCandidates "
            + "from RecruitmentInterviewDetailEntity c "
            + "join c.recruitmentNotification n "
            + "join n.projectMst p "
            + "where n.internalVacancyOpening is not null "
            + "and c.active = true "
            + "and (:searchPattern is null "
            + "or upper(n.requestId) like :searchPattern "
            + "or upper(p.projectName) like :searchPattern)")
    InternalVacancyCandidateRequestSummaryMetricsProjection summarizeInternalVacancyCandidateRequestMetrics(
            @Param("searchPattern") String searchPattern);

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndCandidateEmailIgnoreCase(
            Long recruitmentNotificationId,
            Long agencyId,
            String candidateEmail);

    long countByAgencyAgencyIdAndInterviewDateTimeIsNotNull(Long agencyId);

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndActiveTrue(Long recruitmentNotificationId);

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyIdAndCandidateMobile(
            Long recruitmentNotificationId,
            Long agencyId,
            String candidateMobile);

    Optional<RecruitmentInterviewDetailEntity>
            findByRecruitmentInterviewDetailIdAndRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                    Long recruitmentInterviewDetailId,
                    Long recruitmentNotificationId,
                    Long agencyId);

    @Query("select candidate "
            + "from RecruitmentInterviewDetailEntity candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "join fetch candidate.agency agency "
            + "join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and candidate.agency.agencyId = :agencyId "
            + "and candidate.active = true "
            + "and candidate.finalDecisionStatus = 'SELECTED'")
    Optional<RecruitmentInterviewDetailEntity> findSelectedCandidateByIdAndAgency(
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId,
            @Param("agencyId") Long agencyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "where c.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and c.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and c.recruitmentNotification.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true")
    Optional<RecruitmentInterviewDetailEntity> findByIdForDepartmentReviewUpdate(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join c.recruitmentNotification n "
            + "join n.internalVacancyOpening opening "
            + "join opening.interviewAuthorities authority "
            + "where upper(n.requestId) = upper(:requestId) "
            + "and c.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and authority.user.id = :userId "
            + "and n.internalVacancyOpening is not null "
            + "and n.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and c.active = true")
    Optional<RecruitmentInterviewDetailEntity> findByIdForInternalVacancyInterviewAuthorityReviewUpdate(
            @Param("requestId") String requestId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId,
            @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "join n.internalVacancyOpening opening "
            + "join opening.interviewAuthorities authority "
            + "where upper(n.requestId) = upper(:requestId) "
            + "and c.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and authority.user.id = :userId "
            + "and n.internalVacancyOpening is not null "
            + "and n.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and c.active = true")
    Optional<RecruitmentInterviewDetailEntity> findByIdForInternalVacancyInterviewWorkflowUpdate(
            @Param("requestId") String requestId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId,
            @Param("userId") Long userId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "join fetch c.agency agency "
            + "join n.internalVacancyOpening opening "
            + "join opening.interviewAuthorities authority "
            + "where upper(n.requestId) = upper(:requestId) "
            + "and c.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and authority.user.id = :userId "
            + "and n.internalVacancyOpening is not null "
            + "and n.status <> com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus.CLOSED "
            + "and opening.status = com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus.OPEN "
            + "and c.active = true")
    Optional<RecruitmentInterviewDetailEntity> findByIdForInternalVacancyInterviewWorkflowView(
            @Param("requestId") String requestId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId,
            @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where c.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and n.recruitmentNotificationId = :recruitmentNotificationId "
            + "and n.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true")
    Optional<RecruitmentInterviewDetailEntity> findByIdForDepartmentInterviewWorkflowUpdate(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "join fetch c.agency agency "
            + "where c.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and n.recruitmentNotificationId = :recruitmentNotificationId "
            + "and n.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true")
    Optional<RecruitmentInterviewDetailEntity> findByIdForDepartmentInterviewWorkflowView(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where n.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true "
            + "and c.finalDecisionStatus = 'SELECTED' "
            + "order by c.finalDecisionAt desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findSelectedCandidatesForDepartment(
            @Param("departmentRegistrationId") Long departmentRegistrationId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where n.departmentRegistrationId = :departmentRegistrationId "
            + "and n.recruitmentNotificationId = :recruitmentNotificationId "
            + "and c.active = true "
            + "and c.finalDecisionStatus = 'SELECTED' "
            + "order by c.finalDecisionAt desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findSelectedCandidatesForDepartmentByNotification(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where n.departmentRegistrationId = :departmentRegistrationId "
            + "and c.active = true "
            + "and c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY "
            + "and c.departmentShortlistedAt is not null "
            + "and c.interviewDateTime is not null "
            + "and c.finalDecisionStatus is null "
            + "order by c.interviewDateTime desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findInterviewScheduleAvailableCandidatesForDepartment(
            @Param("departmentRegistrationId") Long departmentRegistrationId);

    @Query("select c "
            + "from RecruitmentInterviewDetailEntity c "
            + "join fetch c.recruitmentNotification n "
            + "join fetch n.projectMst p "
            + "join fetch c.agency agency "
            + "join fetch c.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "where n.departmentRegistrationId = :departmentRegistrationId "
            + "and n.recruitmentNotificationId = :recruitmentNotificationId "
            + "and c.active = true "
            + "and c.candidateStatus = "
            + "com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY "
            + "and c.departmentShortlistedAt is not null "
            + "and c.interviewDateTime is not null "
            + "and c.finalDecisionStatus is null "
            + "order by c.interviewDateTime desc, c.createdDateTime desc")
    List<RecruitmentInterviewDetailEntity> findInterviewScheduleAvailableCandidatesForDepartmentByNotification(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentNotificationId") Long recruitmentNotificationId);
}
