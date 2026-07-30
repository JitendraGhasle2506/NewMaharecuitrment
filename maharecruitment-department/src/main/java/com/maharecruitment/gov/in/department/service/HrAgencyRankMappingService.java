package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.dto.HrAgencyRankRowForm;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingListView;
import com.maharecruitment.gov.in.department.service.model.HrAgencyRankMappingView;
import com.maharecruitment.gov.in.department.service.model.HrRankReleaseRuleListView;

public interface HrAgencyRankMappingService {

    HrAgencyRankMappingListView getAgencyRankMappingListView();

    HrAgencyRankMappingListView getRankReleaseOverviewListView(int pageNumber, int pageSize);

    HrRankReleaseRuleListView getRankReleaseRuleListView();

    HrAgencyRankMappingView getGlobalRankMappingView();

    HrAgencyRankMappingView getRankMappingView(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId);

    void assignGlobalAgencyRanks(List<HrAgencyRankRowForm> rankRows);

    void assignAgencyRanks(
            Long departmentId,
            Long subDepartmentId,
            Long applicationId,
            List<HrAgencyRankRowForm> rankRows);
}
