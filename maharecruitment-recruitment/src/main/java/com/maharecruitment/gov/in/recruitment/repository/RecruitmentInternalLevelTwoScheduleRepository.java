package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;

@Repository
public interface RecruitmentInternalLevelTwoScheduleRepository
        extends JpaRepository<RecruitmentInternalLevelTwoScheduleEntity, Long> {

    Optional<RecruitmentInternalLevelTwoScheduleEntity> findByRecruitmentInterviewDetailRecruitmentInterviewDetailId(
            Long recruitmentInterviewDetailId);

    List<RecruitmentInternalLevelTwoScheduleEntity> findByRecruitmentInterviewDetailRecruitmentInterviewDetailIdIn(
            List<Long> recruitmentInterviewDetailIds);

    @Query(
            value = "select schedule from RecruitmentInternalLevelTwoScheduleEntity schedule "
                    + "join fetch schedule.recruitmentInterviewDetail candidate "
                    + "join fetch candidate.recruitmentNotification notification "
                    + "join fetch notification.projectMst project "
                    + "left join fetch candidate.designationVacancy vacancy "
                    + "left join fetch vacancy.designationMst designation "
                    + "where notification.internalVacancyOpening is not null "
                    + "and candidate.active = true "
                    + "and (:searchPattern is null "
                    + "or upper(notification.requestId) like :searchPattern "
                    + "or upper(project.projectName) like :searchPattern "
                    + "or upper(coalesce(candidate.candidateName, '')) like :searchPattern)",
            countQuery = "select count(schedule) from RecruitmentInternalLevelTwoScheduleEntity schedule "
                    + "join schedule.recruitmentInterviewDetail candidate "
                    + "join candidate.recruitmentNotification notification "
                    + "join notification.projectMst project "
                    + "where notification.internalVacancyOpening is not null "
                    + "and candidate.active = true "
                    + "and (:searchPattern is null "
                    + "or upper(notification.requestId) like :searchPattern "
                    + "or upper(project.projectName) like :searchPattern "
                    + "or upper(coalesce(candidate.candidateName, '')) like :searchPattern)")
    Page<RecruitmentInternalLevelTwoScheduleEntity> findHrScheduledCandidatePage(
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select distinct schedule from RecruitmentInternalLevelTwoScheduleEntity schedule "
            + "join fetch schedule.recruitmentInterviewDetail candidate "
            + "join fetch candidate.recruitmentNotification notification "
            + "join fetch notification.projectMst project "
            + "left join fetch candidate.designationVacancy vacancy "
            + "left join fetch vacancy.designationMst designation "
            + "left join fetch schedule.panelMembers panelMember "
            + "where candidate.recruitmentInterviewDetailId = :recruitmentInterviewDetailId "
            + "and candidate.active = true "
            + "and notification.internalVacancyOpening is not null")
    Optional<RecruitmentInternalLevelTwoScheduleEntity> findDetailedInternalScheduleByCandidateId(
            @Param("recruitmentInterviewDetailId") Long recruitmentInterviewDetailId);
}
