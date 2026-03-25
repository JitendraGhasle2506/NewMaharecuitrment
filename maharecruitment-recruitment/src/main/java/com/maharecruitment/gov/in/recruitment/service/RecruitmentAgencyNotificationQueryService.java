package com.maharecruitment.gov.in.recruitment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;

public interface RecruitmentAgencyNotificationQueryService {

    Page<AgencyVisibleNotificationView> getVisibleNotifications(Long agencyId, String searchText, Pageable pageable);

    AgencyVisibleNotificationListMetricsView getVisibleNotificationMetrics(Long agencyId, String searchText);

    AgencyNotificationDetailView getNotificationDetail(Long recruitmentNotificationId, Long agencyId);
}
