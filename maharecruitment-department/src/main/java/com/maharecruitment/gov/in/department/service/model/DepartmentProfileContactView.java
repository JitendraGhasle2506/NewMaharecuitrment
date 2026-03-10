package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentProfileContactView {

    private String contactName;
    private String designation;
    private String mobileNumber;
    private String emailAddress;
    private boolean primaryContact;
}
