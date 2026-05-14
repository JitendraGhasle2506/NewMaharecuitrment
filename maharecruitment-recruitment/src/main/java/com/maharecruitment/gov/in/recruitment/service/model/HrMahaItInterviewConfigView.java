package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrMahaItInterviewConfigView {

    private Long recruitmentNotificationId;
    private String requestId;
    private String projectName;
    private String status;
    private boolean mahaItInterviewEnabled;
}
