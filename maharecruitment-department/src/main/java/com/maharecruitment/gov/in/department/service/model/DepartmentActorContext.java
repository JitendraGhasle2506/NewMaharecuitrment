package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentActorContext {

    private Long userId;
    private String actorName;
    private String actorEmail;
    private Long departmentId;
    private Long departmentRegistrationId;
    private Long subDepartmentId;
}
