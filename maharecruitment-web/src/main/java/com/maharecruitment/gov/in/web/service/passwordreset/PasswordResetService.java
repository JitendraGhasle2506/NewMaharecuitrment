package com.maharecruitment.gov.in.web.service.passwordreset;

import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetResponse;
import com.maharecruitment.gov.in.web.dto.passwordreset.ResetPasswordRequest;

public interface PasswordResetService {

    PasswordResetResponse requestOtp(
            PasswordResetOtpRequest request,
            ResetPasswordChannel channel,
            String clientIp,
            String userAgent);

    PasswordResetResponse verifyOtp(
            PasswordResetOtpVerifyRequest request,
            ResetPasswordChannel channel,
            String clientIp);

    PasswordResetResponse resetPassword(
            ResetPasswordRequest request,
            ResetPasswordChannel channel,
            String clientIp);

    void invalidateExpiredRequests();
}
