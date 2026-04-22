package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentExternalInterviewPanelMemberEntity;

@Repository
public interface RecruitmentExternalInterviewPanelMemberRepository extends JpaRepository<RecruitmentExternalInterviewPanelMemberEntity, Long> {
    
    List<RecruitmentExternalInterviewPanelMemberEntity> findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(Long recruitmentInterviewDetailId);
    
    void deleteByRecruitmentInterviewDetailRecruitmentInterviewDetailId(Long recruitmentInterviewDetailId);
}
