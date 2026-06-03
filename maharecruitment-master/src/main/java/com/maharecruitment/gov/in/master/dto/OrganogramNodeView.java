package com.maharecruitment.gov.in.master.dto;

import java.util.List;

public record OrganogramNodeView(
        String nodeId,
        String nodeType,
        String label,
        String caption,
        String status,
        String iconCssClass,
        int directChildren,
        int totalProjects,
        List<OrganogramNodeView> children) {

    public OrganogramNodeView {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
