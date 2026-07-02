package com.maharecruitment.gov.in.web.service.mobile;

public interface MobileTokenService {

    MobileTokenIssue issueToken(MobileAuthenticatedUser user);

    MobileTokenClaims validateToken(String token);
}
