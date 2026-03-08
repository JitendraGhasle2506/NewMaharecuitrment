package com.maharecruitment.gov.in.web.service.verification;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

public interface OtpChannelHandler {

    VerificationChannel getChannel();

    String normalizeReference(String reference);

    void dispatchOtp(String reference, String otp);
}
