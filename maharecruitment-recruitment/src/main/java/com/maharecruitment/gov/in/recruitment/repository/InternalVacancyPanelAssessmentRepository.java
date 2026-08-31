package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyPanelAssessmentEntity;

@Repository
public interface InternalVacancyPanelAssessmentRepository extends JpaRepository<InternalVacancyPanelAssessmentEntity, Long> {

    List<InternalVacancyPanelAssessmentEntity> findByInterviewDetailRecruitmentInterviewDetailId(Long interviewDetailId);

    @Query("select count(a) from InternalVacancyPanelAssessmentEntity a " +
           "where a.interviewDetail.recruitmentInterviewDetailId = :interviewDetailId " +
           "and a.status = 'SUBMITTED'")
    long countSubmittedAssessments(@Param("interviewDetailId") Long interviewDetailId);

    @Query("select avg(a.totalScore) from InternalVacancyPanelAssessmentEntity a " +
           "where a.interviewDetail.recruitmentInterviewDetailId = :interviewDetailId " +
           "and a.status = 'SUBMITTED'")
    Double calculateAverageScore(@Param("interviewDetailId") Long interviewDetailId);

    /**
     * Finds the assessment submitted by a specific panel member (identified by userId or employeeId)
     * for a given interview detail. This enforces panel-level data isolation so that each panel
     * can only see their own submitted marks.
     */
    @Query("select a from InternalVacancyPanelAssessmentEntity a " +
           "where a.interviewDetail.recruitmentInterviewDetailId = :interviewDetailId " +
           "and (" +
           "  (:userId is not null and a.assessorUser.id = :userId) " +
           "  or (:employeeId is not null and a.assessorEmployee.employeeId = :employeeId)" +
           ")")
    Optional<InternalVacancyPanelAssessmentEntity> findByInterviewDetailAndAssessor(
            @Param("interviewDetailId") Long interviewDetailId,
            @Param("userId") Long userId,
            @Param("employeeId") Long employeeId);

    @Query("select distinct a.interviewDetail.recruitmentInterviewDetailId " +
           "from InternalVacancyPanelAssessmentEntity a " +
           "where a.interviewDetail.recruitmentInterviewDetailId in :interviewDetailIds " +
           "and a.status = 'SUBMITTED' " +
           "and (" +
           "  (:userId is not null and a.assessorUser.id = :userId) " +
           "  or (:employeeId is not null and a.assessorEmployee.employeeId = :employeeId)" +
           ")")
    Set<Long> findSubmittedInterviewDetailIdsByAssessor(
            @Param("interviewDetailIds") Set<Long> interviewDetailIds,
            @Param("userId") Long userId,
            @Param("employeeId") Long employeeId);
}
