package com.maharecruitment.gov.in.web.service.navigation;

import java.util.List;

public interface NavigationService {

    String resolveHomeUrl(List<String> roles);

    String resolvePrimaryRoleLabel(List<String> roles);
}
