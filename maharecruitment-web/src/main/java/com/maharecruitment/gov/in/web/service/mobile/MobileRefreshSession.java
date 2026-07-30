package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.auth.entity.User;

public record MobileRefreshSession(
        User user,
        MobileRefreshTokenIssue refreshToken) {
}
