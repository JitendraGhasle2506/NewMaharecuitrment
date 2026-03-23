package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyInterviewAuthorityRoleOptionView {

    private Long roleId;
    private String roleName;
    private String roleLabel;
}
