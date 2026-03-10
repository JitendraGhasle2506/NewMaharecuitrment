package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditorDepartmentRegistrationDetailView {

    private String departmentName;
    private String subDepartmentName;
    private String billingDepartmentName;
    private String billingAddress;
    private String gstNumber;
    private String panNumber;
    private String tanNumber;
    private String gstDocumentName;
    private String panDocumentName;
    private String tanDocumentName;
    private boolean gstDocumentAvailable;
    private boolean panDocumentAvailable;
    private boolean tanDocumentAvailable;
}
