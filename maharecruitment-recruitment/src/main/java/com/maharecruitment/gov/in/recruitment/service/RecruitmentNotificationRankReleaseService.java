package com.maharecruitment.gov.in.recruitment.service;

public interface RecruitmentNotificationRankReleaseService {

    int releaseEligibleNotifications();

    int releaseEligibleRanksForNotification(Long recruitmentNotificationId);
}

