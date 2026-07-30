package com.maharecruitment.gov.in.workorder.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderAgencyOptionView {

    private Long agencyId;
    private String agencyName;

    public Long agencyId() {
        return agencyId;
    }

    public String agencyName() {
        return agencyName;
    }
}
