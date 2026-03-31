package com.maharecruitment.gov.in.department.service.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrAgencyRankMappingListView {

    private List<HrAgencyRankMappingListRowView> rankMappings;

    private List<HrAgencyRankReleaseOverviewGroupView> releaseGroups;

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private boolean hasPrevious;

    private boolean hasNext;

    private int showingFrom;

    private int showingTo;
}
