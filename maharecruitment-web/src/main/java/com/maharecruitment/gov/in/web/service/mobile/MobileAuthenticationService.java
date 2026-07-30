package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLogoutRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLogoutResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileRefreshTokenRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileRefreshTokenResponse;

public interface MobileAuthenticationService {

    MobileLoginResponse authenticate(MobileLoginRequest request);

    MobileRefreshTokenResponse refresh(MobileRefreshTokenRequest request);

    MobileLogoutResponse logout(MobileLogoutRequest request);
}
