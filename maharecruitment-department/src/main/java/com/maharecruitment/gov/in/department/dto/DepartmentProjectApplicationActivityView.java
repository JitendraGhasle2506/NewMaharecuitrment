package com.maharecruitment.gov.in.department.dto;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationActivityType;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentProjectApplicationActivityView {

    private DepartmentApplicationActivityType activityType;
    private DepartmentApplicationStatus previousStatus;
    private DepartmentApplicationStatus newStatus;
    private String actorName;
    private String actorEmail;
    private String activityRemarks;
    private LocalDateTime actionTimestamp;
}
