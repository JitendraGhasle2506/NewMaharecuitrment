package com.maharecruitment.gov.in.recruitment.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankReleaseService;

@Component
public class RecruitmentNotificationRankReleaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentNotificationRankReleaseScheduler.class);

    private final RecruitmentNotificationRankReleaseService rankReleaseService;

    public RecruitmentNotificationRankReleaseScheduler(
            RecruitmentNotificationRankReleaseService rankReleaseService) {
        this.rankReleaseService = rankReleaseService;
    }

    @Scheduled(
            initialDelayString = "${recruitment.rank-release.scheduler.initial-delay-ms:30000}",
            fixedDelayString = "${recruitment.rank-release.scheduler.fixed-delay-ms:300000}")
    public void releaseEligibleRanks() {
        try {
            int releasedCount = rankReleaseService.releaseEligibleNotifications();
            if (releasedCount > 0) {
                log.info("Recruitment rank release scheduler completed. releasedRankCount={}", releasedCount);
            }
        } catch (Exception ex) {
            log.error("Recruitment rank release scheduler failed.", ex);
        }
    }
}

