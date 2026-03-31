package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.repository.projection.AgencyVisibleNotificationMetricsProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.AgencyVisibleNotificationProjection;

@Repository
public interface AgencyNotificationTrackingRepository extends JpaRepository<AgencyNotificationTrackingEntity, Long> {

    boolean existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
            Long recruitmentNotificationId,
            Long agencyId);

    long countByAgencyAgencyIdAndStatus(Long agencyId, AgencyNotificationTrackingStatus status);

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

    @Query(
            value = "select notification.recruitmentNotificationId as recruitmentNotificationId, "
                    + "notification.requestId as requestId, "
                    + "notification.departmentRegistrationId as departmentRegistrationId, "
                    + "notification.departmentProjectApplicationId as departmentProjectApplicationId, "
                    + "project.projectId as projectId, "
                    + "project.projectName as projectName, "
                    + "tracking.releasedRank as releasedRank, "
                    + "tracking.notifiedAt as notifiedAt, "
                    + "tracking.status as trackingStatus, "
                    + "notification.status as notificationStatus "
                    + "from AgencyNotificationTrackingEntity tracking "
                    + "join tracking.recruitmentNotification notification "
                    + "left join notification.projectMst project "
                    + "where tracking.agency.agencyId = :agencyId "
                    + "and tracking.status in :trackingStatuses "
                    + "and notification.status in :notificationStatuses "
                    + "and (:searchPattern is null "
                    + "or upper(notification.requestId) like :searchPattern "
                    + "or upper(project.projectName) like :searchPattern) "
                    + "order by tracking.notifiedAt desc",
            countQuery = "select count(tracking) "
                    + "from AgencyNotificationTrackingEntity tracking "
                    + "join tracking.recruitmentNotification notification "
                    + "left join notification.projectMst project "
                    + "where tracking.agency.agencyId = :agencyId "
                    + "and tracking.status in :trackingStatuses "
                    + "and notification.status in :notificationStatuses "
                    + "and (:searchPattern is null "
                    + "or upper(notification.requestId) like :searchPattern "
                    + "or upper(project.projectName) like :searchPattern)")
    Page<AgencyVisibleNotificationProjection> findVisibleNotificationPageByAgency(
            @Param("agencyId") Long agencyId,
            @Param("trackingStatuses") Collection<AgencyNotificationTrackingStatus> trackingStatuses,
            @Param("notificationStatuses") Collection<RecruitmentNotificationStatus> notificationStatuses,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select count(tracking) as totalNotifications, "
            + "coalesce(sum(case when tracking.status = "
            + "com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus.RELEASED "
            + "then 1 else 0 end), 0) as releasedNotifications, "
            + "coalesce(sum(case when tracking.status = "
            + "com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus.READ "
            + "then 1 else 0 end), 0) as readNotifications, "
            + "coalesce(sum(case when tracking.status = "
            + "com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus.RESPONDED "
            + "then 1 else 0 end), 0) as respondedNotifications "
            + "from AgencyNotificationTrackingEntity tracking "
            + "join tracking.recruitmentNotification notification "
            + "left join notification.projectMst project "
            + "where tracking.agency.agencyId = :agencyId "
            + "and tracking.status in :trackingStatuses "
            + "and notification.status in :notificationStatuses "
            + "and (:searchPattern is null "
            + "or upper(notification.requestId) like :searchPattern "
            + "or upper(project.projectName) like :searchPattern)")
    AgencyVisibleNotificationMetricsProjection summarizeVisibleNotificationMetricsByAgency(
            @Param("agencyId") Long agencyId,
            @Param("trackingStatuses") Collection<AgencyNotificationTrackingStatus> trackingStatuses,
            @Param("notificationStatuses") Collection<RecruitmentNotificationStatus> notificationStatuses,
            @Param("searchPattern") String searchPattern);

    @Query("select tracking "
            + "from AgencyNotificationTrackingEntity tracking "
            + "join fetch tracking.recruitmentNotification notification "
            + "join fetch tracking.agency agency "
            + "where notification.recruitmentNotificationId in :recruitmentNotificationIds")
    List<AgencyNotificationTrackingEntity> findByRecruitmentNotificationIds(
            @Param("recruitmentNotificationIds") Collection<Long> recruitmentNotificationIds);
}
