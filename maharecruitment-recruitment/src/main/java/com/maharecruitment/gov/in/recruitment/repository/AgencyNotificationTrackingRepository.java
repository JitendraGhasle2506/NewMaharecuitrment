package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;

@Repository
public interface AgencyNotificationTrackingRepository extends JpaRepository<AgencyNotificationTrackingEntity, Long> {

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
            Long recruitmentNotificationId,
            Long agencyId);

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndStatus(
            Long recruitmentNotificationId,
            AgencyNotificationTrackingStatus status);

    Optional<AgencyNotificationTrackingEntity> findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
            Long recruitmentNotificationId,
            Long agencyId);

    @Query("select max(tracking.releasedRank) "
            + "from AgencyNotificationTrackingEntity tracking "
            + "where tracking.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId")
    Integer findMaxReleasedRankByNotificationId(@Param("recruitmentNotificationId") Long recruitmentNotificationId);

    @Query("select min(tracking.notifiedAt) "
            + "from AgencyNotificationTrackingEntity tracking "
            + "where tracking.recruitmentNotification.recruitmentNotificationId = :recruitmentNotificationId "
            + "and tracking.releasedRank = :releasedRank")
    LocalDateTime findFirstNotifiedAtForRank(
            @Param("recruitmentNotificationId") Long recruitmentNotificationId,
            @Param("releasedRank") Integer releasedRank);

    @Query("select tracking "
            + "from AgencyNotificationTrackingEntity tracking "
            + "join fetch tracking.recruitmentNotification notification "
            + "left join fetch notification.projectMst project "
            + "where tracking.agency.agencyId = :agencyId "
            + "and tracking.status in :trackingStatuses "
            + "and notification.status in :notificationStatuses "
            + "order by tracking.notifiedAt desc")
    List<AgencyNotificationTrackingEntity> findVisibleTrackingByAgency(
            @Param("agencyId") Long agencyId,
            @Param("trackingStatuses") Collection<AgencyNotificationTrackingStatus> trackingStatuses,
            @Param("notificationStatuses") Collection<RecruitmentNotificationStatus> notificationStatuses);
}
