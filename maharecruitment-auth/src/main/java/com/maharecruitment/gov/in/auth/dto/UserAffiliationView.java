package com.maharecruitment.gov.in.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAffiliationView {

    private Long userId;
    private String name;
    private String email;
    private String mobileNo;
    private Long departmentRegistrationId;
    private Long departmentId;
    private Long subDepartmentId;
    private String departmentName;
    private Long agencyId;
    private List<String> roleNames;
}
