package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrDepartmentRequestSummaryView {

    private Long departmentRegistrationId;
    private String departmentName;
    private String primaryContactName;
    private String primaryContactEmail;
    private String primaryContactMobile;
    private Long submittedProjectCount;
    private LocalDateTime registrationDate;
    private boolean active;
}
