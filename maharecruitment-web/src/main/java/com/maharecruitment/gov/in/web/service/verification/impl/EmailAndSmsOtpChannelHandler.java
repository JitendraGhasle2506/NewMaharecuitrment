package com.maharecruitment.gov.in.web.service.verification.impl;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.sms.service.SmsService;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryReferences;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryReferences.BothReference;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryException;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

@Component
public class EmailAndSmsOtpChannelHandler implements OtpChannelHandler {

    private final OtpDispatchService otpDispatchService;
    private final SmsService smsService;

    public EmailAndSmsOtpChannelHandler(
            OtpDispatchService otpDispatchService,
            SmsService smsService) {
        this.otpDispatchService = otpDispatchService;
        this.smsService = smsService;
    }

    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.BOTH;
    }

    @Override
    public String normalizeReference(String reference) {
        BothReference bothReference = OtpDeliveryReferences.parseBoth(reference);
        return OtpDeliveryReferences.both(bothReference.email(), bothReference.mobileNumber());
    }

    @Override
    public void dispatchOtp(String purpose, String reference, String otp) {
        dispatchOtp(purpose, reference, otp, null);
    }

    @Override
    public void dispatchOtp(String purpose, String reference, String otp, String otpReferenceId) {
        BothReference bothReference = OtpDeliveryReferences.parseBoth(reference);
        RuntimeException emailFailure = null;
        RuntimeException smsFailure = null;

        try {
            otpDispatchService.sendEmailOtp(bothReference.email(), otp, purpose, otpReferenceId);
        } catch (RuntimeException exception) {
            emailFailure = exception;
        }

        try {
            if (VerificationPurposes.LOGIN_AUTHENTICATION.equalsIgnoreCase(purpose)) {
                smsService.sendLoginOtp(null, bothReference.mobileNumber(), otp);
            } else {
                throw new IllegalArgumentException("SMS template is not configured for this OTP purpose.");
            }
        } catch (RuntimeException exception) {
            smsFailure = exception;
        }

        if (emailFailure != null && smsFailure != null) {
            OtpDeliveryException failure = new OtpDeliveryException(
                    VerificationChannel.BOTH,
                    "Failed to deliver OTP through email and SMS.",
                    emailFailure);
            failure.addSuppressed(smsFailure);
            throw failure;
        }
    }
}
