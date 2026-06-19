package com.maharecruitment.gov.in.recruitment.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationLookupOption {

    private Long id;
    private String label;
    private String code;
    private String type;
    private Long cellId;
}
