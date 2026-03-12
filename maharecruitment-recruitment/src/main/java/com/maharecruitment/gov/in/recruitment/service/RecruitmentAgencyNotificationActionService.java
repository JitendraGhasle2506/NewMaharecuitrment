package com.maharecruitment.gov.in.recruitment.service;

public interface RecruitmentAgencyNotificationActionService {

    void markAsRead(Long recruitmentNotificationId, Long agencyId);

    void submitResponse(Long recruitmentNotificationId, Long agencyId);
}

