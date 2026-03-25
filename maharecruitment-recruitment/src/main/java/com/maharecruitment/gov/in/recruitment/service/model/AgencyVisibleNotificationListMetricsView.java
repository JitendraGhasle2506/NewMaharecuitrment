package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgencyVisibleNotificationListMetricsView {

    private long totalNotifications;
    private long releasedNotifications;
    private long readNotifications;
    private long respondedNotifications;
}
