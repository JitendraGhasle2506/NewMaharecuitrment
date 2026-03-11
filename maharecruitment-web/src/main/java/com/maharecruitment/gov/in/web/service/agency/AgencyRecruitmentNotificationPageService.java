package com.maharecruitment.gov.in.web.service.agency;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;

public interface AgencyRecruitmentNotificationPageService {

    List<AgencyVisibleNotificationView> getVisibleNotifications(String actorEmail);

    AgencyNotificationDetailView getNotificationDetail(String actorEmail, Long recruitmentNotificationId);

    void markAsRead(String actorEmail, Long recruitmentNotificationId);

    void submitResponse(String actorEmail, Long recruitmentNotificationId);
}
