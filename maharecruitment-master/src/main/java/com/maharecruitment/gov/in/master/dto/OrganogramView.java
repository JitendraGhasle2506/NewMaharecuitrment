package com.maharecruitment.gov.in.master.dto;

public record OrganogramView(
        OrganogramNodeView root,
        int wingCount,
        int cellCount,
        int projectCount,
        int hierarchyDepth) {
}
