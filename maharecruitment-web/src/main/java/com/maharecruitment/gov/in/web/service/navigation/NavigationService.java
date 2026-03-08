package com.maharecruitment.gov.in.web.service.navigation;

import java.util.List;

import com.maharecruitment.gov.in.web.service.navigation.model.SidebarItemView;

public interface NavigationService {

    String resolveHomeUrl(List<String> roles);

    List<SidebarItemView> resolveSidebarItems(List<String> roles);

    String resolvePrimaryRoleLabel(List<String> roles);
}
