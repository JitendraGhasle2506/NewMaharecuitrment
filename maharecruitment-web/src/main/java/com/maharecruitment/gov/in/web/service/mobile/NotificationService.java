package com.maharecruitment.gov.in.web.service.mobile;

import com.maharecruitment.gov.in.web.dto.mobile.FcmTokenRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileFcmTokenResponse;

/**
 * Handles mobile notification token registration.
 */
public interface NotificationService {

    /**
     * Saves a new employee FCM token or updates the token for an existing employee-device pair.
     *
     * @param request FCM token registration request
     * @return success response with create or update message
     */
    MobileFcmTokenResponse saveToken(FcmTokenRequest request);
}
