package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.auth.entity.User;

public interface MobileRefreshTokenService {

    MobileRefreshTokenIssue issueRefreshToken(User user);

    MobileRefreshSession rotateRefreshToken(String refreshToken);
}
