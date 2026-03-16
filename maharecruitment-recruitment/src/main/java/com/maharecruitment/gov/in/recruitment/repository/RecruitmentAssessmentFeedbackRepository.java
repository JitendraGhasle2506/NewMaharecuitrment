package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentAssessmentFeedbackEntity;

@Repository
public interface RecruitmentAssessmentFeedbackRepository extends JpaRepository<RecruitmentAssessmentFeedbackEntity, Long> {

    Optional<RecruitmentAssessmentFeedbackEntity> findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(
            Long recruitmentInterviewDetailId);

    @Query("select feedback "
            + "from RecruitmentAssessmentFeedbackEntity feedback "
            + "left join fetch feedback.panelMembers panelMember "
            + "where feedback.recruitmentInterviewDetail.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and feedback.departmentRegistrationId = :departmentRegistrationId")
    Optional<RecruitmentAssessmentFeedbackEntity> findByCandidateForDepartment(
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);
}
