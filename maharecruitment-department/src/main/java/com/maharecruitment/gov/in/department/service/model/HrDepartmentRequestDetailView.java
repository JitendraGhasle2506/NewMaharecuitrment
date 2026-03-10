package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrDepartmentRequestDetailView {

    private HrDepartmentRequestSummaryView summary;
    private List<HrDepartmentSubmittedApplicationView> submittedApplications;
}
