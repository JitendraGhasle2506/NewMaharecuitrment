package com.maharecruitment.gov.in.auth.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpsertRequest {

    private String name;
    private String email;
    private String mobileNo;
    private String password;
    private Long departmentRegistrationId;
    private Long agencyId;
    private List<Long> roleIds = new ArrayList<>();
}
