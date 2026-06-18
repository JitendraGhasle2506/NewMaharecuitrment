package com.maharecruitment.gov.in.recruitment.service.organization;

import java.util.List;

import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationChartNodeResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationDashboardResponse;
import com.maharecruitment.gov.in.recruitment.dto.organization.OrganizationSearchResult;

public interface OrganizationHierarchyService {

    OrganizationDashboardResponse getDashboard(Long projectId, Long cellId);

    OrganizationChartNodeResponse getTree(Long projectId, Long cellId);

    OrganizationChartNodeResponse getOrganizationChart(Long projectId, Long cellId);

    List<OrganizationSearchResult> search(Long projectId, Long cellId, String search);
}
