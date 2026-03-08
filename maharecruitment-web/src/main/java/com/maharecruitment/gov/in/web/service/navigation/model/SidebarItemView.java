package com.maharecruitment.gov.in.web.service.navigation.model;

public record SidebarItemView(
        String label,
        String iconClass,
        String url
) {
    public SidebarItemView withUrl(String resolvedUrl) {
        return new SidebarItemView(label, iconClass, resolvedUrl);
    }
}
