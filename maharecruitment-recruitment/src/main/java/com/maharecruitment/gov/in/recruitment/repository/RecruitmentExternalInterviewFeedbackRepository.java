package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentExternalInterviewFeedbackEntity;

@Repository
public interface RecruitmentExternalInterviewFeedbackRepository extends JpaRepository<RecruitmentExternalInterviewFeedbackEntity, Long> {
    
    List<RecruitmentExternalInterviewFeedbackEntity> findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(Long recruitmentInterviewDetailId);

    long countByRecruitmentInterviewDetailRecruitmentInterviewDetailId(Long recruitmentInterviewDetailId);

    java.util.Optional<RecruitmentExternalInterviewFeedbackEntity> findByRecruitmentInterviewDetailRecruitmentInterviewDetailIdAndReviewerUserId(Long recruitmentInterviewDetailId, Long reviewerUserId);
}
