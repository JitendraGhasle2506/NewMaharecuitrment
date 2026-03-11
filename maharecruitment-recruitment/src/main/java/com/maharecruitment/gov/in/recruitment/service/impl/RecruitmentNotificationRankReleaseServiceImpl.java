package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RankReleaseRuleEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationAgencyRankEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RankReleaseRuleRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationAgencyRankRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankReleaseService;

@Service
@Transactional(readOnly = true)
public class RecruitmentNotificationRankReleaseServiceImpl implements RecruitmentNotificationRankReleaseService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentNotificationRankReleaseServiceImpl.class);

    private static final int RELEASE_BATCH_SIZE = 200;
    private static final Set<RecruitmentNotificationStatus> RELEASABLE_NOTIFICATION_STATUSES = Set.of(
            RecruitmentNotificationStatus.PENDING_ALLOCATION,
            RecruitmentNotificationStatus.IN_PROGRESS);

    private final RecruitmentNotificationRepository notificationRepository;
    private final RecruitmentNotificationAgencyRankRepository agencyRankRepository;
    private final RankReleaseRuleRepository rankReleaseRuleRepository;
    private final AgencyNotificationTrackingRepository trackingRepository;

    public RecruitmentNotificationRankReleaseServiceImpl(
            RecruitmentNotificationRepository notificationRepository,
            RecruitmentNotificationAgencyRankRepository agencyRankRepository,
            RankReleaseRuleRepository rankReleaseRuleRepository,
            AgencyNotificationTrackingRepository trackingRepository) {
        this.notificationRepository = notificationRepository;
        this.agencyRankRepository = agencyRankRepository;
        this.rankReleaseRuleRepository = rankReleaseRuleRepository;
        this.trackingRepository = trackingRepository;
    }

    @Override
    @Transactional
    public int releaseEligibleNotifications() {
        int releasedRankCount = 0;
        Pageable pageable = PageRequest.of(0, RELEASE_BATCH_SIZE);

        Page<RecruitmentNotificationEntity> pageResult;
        do {
            pageResult = notificationRepository.findByStatusIn(RELEASABLE_NOTIFICATION_STATUSES, pageable);
            for (RecruitmentNotificationEntity notification : pageResult.getContent()) {
                releasedRankCount += releaseEligibleRanksForNotification(notification.getRecruitmentNotificationId());
            }
            pageable = pageResult.nextPageable();
        } while (pageResult.hasNext());

        return releasedRankCount;
    }

    @Override
    @Transactional
    public int releaseEligibleRanksForNotification(Long recruitmentNotificationId) {
        if (recruitmentNotificationId == null) {
            throw new RecruitmentNotificationException("Recruitment notification id is required.");
        }

        RecruitmentNotificationEntity notification = notificationRepository
                .findByIdForUpdate(recruitmentNotificationId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Recruitment notification not found for id: " + recruitmentNotificationId));

        if (!RELEASABLE_NOTIFICATION_STATUSES.contains(notification.getStatus())) {
            return 0;
        }

        if (trackingRepository.existsByRecruitmentNotificationRecruitmentNotificationIdAndStatus(
                recruitmentNotificationId,
                AgencyNotificationTrackingStatus.RESPONDED)) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int releasedRankCount = 0;

        while (true) {
            Integer maxReleasedRank = trackingRepository.findMaxReleasedRankByNotificationId(recruitmentNotificationId);
            Integer nextRank = resolveNextRank(recruitmentNotificationId, maxReleasedRank);
            if (nextRank == null) {
                break;
            }

            if (!isRankEligibleForRelease(recruitmentNotificationId, maxReleasedRank, nextRank, now)) {
                break;
            }

            int releasedAgencyCount = releaseRank(notification, nextRank, now);
            if (releasedAgencyCount <= 0) {
                break;
            }

            releasedRankCount++;

            if (trackingRepository.existsByRecruitmentNotificationRecruitmentNotificationIdAndStatus(
                    recruitmentNotificationId,
                    AgencyNotificationTrackingStatus.RESPONDED)) {
                break;
            }
        }

        return releasedRankCount;
    }

    private Integer resolveNextRank(Long recruitmentNotificationId, Integer currentRank) {
        if (currentRank == null) {
            return agencyRankRepository.findMinimumRankByNotificationId(recruitmentNotificationId);
        }
        return agencyRankRepository.findNextHigherRankByNotificationId(recruitmentNotificationId, currentRank);
    }

    private boolean isRankEligibleForRelease(
            Long recruitmentNotificationId,
            Integer currentReleasedRank,
            Integer nextRank,
            LocalDateTime now) {
        if (currentReleasedRank == null) {
            return true;
        }

        LocalDateTime currentRankReleaseTime = trackingRepository.findFirstNotifiedAtForRank(
                recruitmentNotificationId,
                currentReleasedRank);
        if (currentRankReleaseTime == null) {
            return false;
        }

        RankReleaseRuleEntity releaseRule = rankReleaseRuleRepository
                .findByRankNumber(nextRank)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Rank release rule not found for rank: " + nextRank));

        LocalDateTime nextEligibleTime = currentRankReleaseTime.plusDays(releaseRule.getEffectiveReleaseDelayDays());
        return !now.isBefore(nextEligibleTime);
    }

    private int releaseRank(
            RecruitmentNotificationEntity notification,
            Integer rankNumber,
            LocalDateTime releaseTimestamp) {
        Long recruitmentNotificationId = notification.getRecruitmentNotificationId();
        List<RecruitmentNotificationAgencyRankEntity> mappings = agencyRankRepository
                .findByRecruitmentNotificationRecruitmentNotificationIdAndRankNumberOrderByAgencyAgencyIdAsc(
                        recruitmentNotificationId,
                        rankNumber);

        if (mappings.isEmpty()) {
            return 0;
        }

        int releasedAgencyCount = 0;
        for (RecruitmentNotificationAgencyRankEntity mapping : mappings) {
            Long agencyId = mapping.getAgency().getAgencyId();
            if (trackingRepository.existsByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                    recruitmentNotificationId,
                    agencyId)) {
                continue;
            }

            AgencyNotificationTrackingEntity tracking = new AgencyNotificationTrackingEntity();
            tracking.setRecruitmentNotification(notification);
            tracking.setAgency(mapping.getAgency());
            tracking.setReleasedRank(rankNumber);
            tracking.setNotifiedAt(releaseTimestamp);
            tracking.setStatus(AgencyNotificationTrackingStatus.RELEASED);
            trackingRepository.save(tracking);
            releasedAgencyCount++;
        }

        if (releasedAgencyCount > 0 && notification.getStatus() != RecruitmentNotificationStatus.IN_PROGRESS) {
            notification.setStatus(RecruitmentNotificationStatus.IN_PROGRESS);
            notificationRepository.save(notification);
        }

        if (releasedAgencyCount > 0) {
            log.info(
                    "Recruitment notification rank released. notificationId={}, rank={}, releasedAgencyCount={}",
                    recruitmentNotificationId,
                    rankNumber,
                    releasedAgencyCount);
        }

        return releasedAgencyCount;
    }
}
