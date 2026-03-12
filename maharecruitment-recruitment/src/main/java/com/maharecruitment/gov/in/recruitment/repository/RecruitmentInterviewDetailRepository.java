package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.repository.projection.DepartmentNotificationCandidateSummaryProjection;

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
}
