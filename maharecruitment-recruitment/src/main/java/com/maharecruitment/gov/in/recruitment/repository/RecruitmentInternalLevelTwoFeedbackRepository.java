package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoFeedbackEntity;

@Repository
public interface RecruitmentInternalLevelTwoFeedbackRepository
        extends JpaRepository<RecruitmentInternalLevelTwoFeedbackEntity, Long> {

    @Query("select feedback "
            + "from RecruitmentInternalLevelTwoFeedbackEntity feedback "
            + "join fetch feedback.schedule schedule "
            + "join fetch schedule.recruitmentInterviewDetail candidate "
            + "where candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "order by feedback.submittedAt desc")
    List<RecruitmentInternalLevelTwoFeedbackEntity> findByCandidateId(
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);

    @Query("select feedback "
            + "from RecruitmentInternalLevelTwoFeedbackEntity feedback "
            + "join fetch feedback.schedule schedule "
            + "join fetch schedule.recruitmentInterviewDetail candidate "
            + "where candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and feedback.reviewerUserId = :reviewerUserId")
    Optional<RecruitmentInternalLevelTwoFeedbackEntity> findByCandidateIdAndReviewerUserId(
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId,
            @Param("reviewerUserId") Long reviewerUserId);

    @Query("select feedback "
            + "from RecruitmentInternalLevelTwoFeedbackEntity feedback "
            + "join fetch feedback.schedule schedule "
            + "join fetch schedule.recruitmentInterviewDetail candidate "
            + "where candidate.recruitmentInterviewDetailId in :recruitmentInterviewDetailIds "
            + "order by feedback.submittedAt desc")
    List<RecruitmentInternalLevelTwoFeedbackEntity> findByCandidateIds(
            @Param("recruitmentInterviewDetailIds") List<Long> recruitmentInterviewDetailIds);

    @Query("select feedback "
            + "from RecruitmentInternalLevelTwoFeedbackEntity feedback "
            + "join fetch feedback.schedule schedule "
            + "join fetch schedule.recruitmentInterviewDetail candidate "
            + "where candidate.recruitmentInterviewDetailId in :recruitmentInterviewDetailIds "
            + "and feedback.reviewerUserId = :reviewerUserId")
    List<RecruitmentInternalLevelTwoFeedbackEntity> findByCandidateIdsAndReviewerUserId(
            @Param("recruitmentInterviewDetailIds") List<Long> recruitmentInterviewDetailIds,
            @Param("reviewerUserId") Long reviewerUserId);
}
