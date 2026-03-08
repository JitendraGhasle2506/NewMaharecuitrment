package com.maharecruitment.gov.in.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentUserProvisioningResult {

    private Long userId;
    private String email;
    private String temporaryPassword;
}
