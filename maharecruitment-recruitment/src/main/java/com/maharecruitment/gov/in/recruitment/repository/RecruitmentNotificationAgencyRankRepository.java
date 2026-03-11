package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationAgencyRankEntity;

@Repository
public interface RecruitmentNotificationAgencyRankRepository
        extends JpaRepository<RecruitmentNotificationAgencyRankEntity, Long> {

    @Query("""
            select mapping
            from RecruitmentNotificationAgencyRankEntity mapping
            join fetch mapping.recruitmentNotification notification
            join fetch mapping.agency agency
            left join fetch notification.projectMst project
            order by mapping.assignedDate desc, mapping.recruitmentNotificationAgencyRankId desc
            """)
    List<RecruitmentNotificationAgencyRankEntity> findAllWithNotificationAndAgency();

    List<RecruitmentNotificationAgencyRankEntity>
            findByRecruitmentNotificationRecruitmentNotificationIdOrderByRankNumberAscAgencyAgencyIdAsc(
                    Long recruitmentNotificationId);

    List<RecruitmentNotificationAgencyRankEntity>
            findByRecruitmentNotificationRecruitmentNotificationIdAndRankNumberOrderByAgencyAgencyIdAsc(
                    Long recruitmentNotificationId,
                    Integer rankNumber);

    Optional<RecruitmentNotificationAgencyRankEntity>
            findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                    Long recruitmentNotificationId,
                    Long agencyId);

    @Query("""
            select min(mapping.rankNumber)
            from RecruitmentNotificationAgencyRankEntity mapping
            where mapping.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId
            """)
    Integer findMinimumRankByNotificationId(@Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("""
            select min(mapping.rankNumber)
            from RecruitmentNotificationAgencyRankEntity mapping
            where mapping.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId
              and mapping.rankNumber > :currentRank
            """)
    Integer findNextHigherRankByNotificationId(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("currentRank") Integer currentRank);
}
