package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileLoginResponse;

public interface MobileAuthenticationService {

    MobileLoginResponse authenticate(MobileLoginRequest request);
}
