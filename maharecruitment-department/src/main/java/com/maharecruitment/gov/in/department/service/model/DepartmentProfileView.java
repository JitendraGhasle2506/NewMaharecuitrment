package com.maharecruitment.gov.in.department.service.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentProfileView {

    private Long departmentRegistrationId;
    private String departmentName;
    private Long departmentMasterId;
    private String subDepartmentName;
    private Long subDepartmentMasterId;
    private String officeAddress;
    private String billDepartmentName;
    private String gstNumber;
    private String panNumber;
    private String tanNumber;
    private String billingAddress;
    private String gstDocumentName;
    private String panDocumentName;
    private String tanDocumentName;
    private boolean active;
    private boolean termsAccepted;
    private LocalDateTime registeredOn;
    private String loginUserName;
    private String loginUserEmail;
    private String loginUserMobile;
    private DepartmentProfileContactView primaryContact;
    private DepartmentProfileContactView secondaryContact;
}
