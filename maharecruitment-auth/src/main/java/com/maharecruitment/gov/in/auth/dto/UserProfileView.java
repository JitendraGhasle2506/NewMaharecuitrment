package com.maharecruitment.gov.in.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileView {

    private Long id;
    private String name;
    private String email;
    private String mobileNo;
    private Long departmentRegistrationId;
    private String departmentName;
    private List<String> roleNames;
}
