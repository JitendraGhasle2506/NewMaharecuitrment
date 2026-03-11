package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.dto.HrAgencyRankRowForm;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingListView;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingView;

public interface HrAgencyRankMappingService {

    HrAgencyRankMappingListView getAgencyRankMappingListView();

    HrAgencyRankMappingView getRankMappingView(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId);

    void assignAgencyRanks(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            List<HrAgencyRankRowForm> rankRows);
}
