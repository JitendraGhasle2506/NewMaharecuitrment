package com.maharecruitment.gov.in.recruitment.repository.projection;

public interface AgencyVisibleNotificationMetricsProjection {

    Long getTotalNotifications();

    Long getReleasedNotifications();

    Long getReadNotifications();

    Long getRespondedNotifications();
}
