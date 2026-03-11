package com.maharecruitment.gov.in.recruitment.service;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;

public interface RecruitmentAgencyNotificationQueryService {

    List<AgencyVisibleNotificationView> getVisibleNotifications(Long agencyId);

    AgencyNotificationDetailView getNotificationDetail(Long recruitmentNotificationId, Long agencyId);
}

