package com.maharecruitment.gov.in.master.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyEscalationMatrixResponse {

    private Long escalationMatrixId;
    private String contactName;
    private String mobileNumber;
    private String level;
    private String designation;
    private String companyEmailId;
}
