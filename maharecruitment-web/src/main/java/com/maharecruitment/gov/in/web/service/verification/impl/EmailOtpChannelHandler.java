package com.maharecruitment.gov.in.web.service.verification.impl;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryReferences;
import com.maharecruitment.gov.in.web.service.verification.OtpDeliveryException;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;

@Component
public class EmailOtpChannelHandler implements OtpChannelHandler {

    private final OtpDispatchService otpDispatchService;

    public EmailOtpChannelHandler(OtpDispatchService otpDispatchService) {
        this.otpDispatchService = otpDispatchService;
    }

    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.EMAIL;
    }

    @Override
    public String normalizeReference(String reference) {
        return OtpDeliveryReferences.normalizeEmail(reference);
    }

    @Override
    public void dispatchOtp(String purpose, String reference, String otp) {
        dispatchOtp(purpose, reference, otp, null);
    }

    @Override
    public void dispatchOtp(String purpose, String reference, String otp, String otpReferenceId) {
        try {
            otpDispatchService.sendEmailOtp(reference, otp, purpose, otpReferenceId);
        } catch (OtpDeliveryException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new OtpDeliveryException(
                    VerificationChannel.EMAIL,
                    "Email OTP delivery failed.",
                    ex);
        }
    }
}
