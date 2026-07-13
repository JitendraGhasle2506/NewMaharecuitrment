package com.maharecruitment.gov.in.web.service.verification.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.common.sms.service.SmsService;
import com.maharecruitment.gov.in.common.sms.template.SmsTemplateCode;
import com.maharecruitment.gov.in.common.sms.util.MobileNumberUtil;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpChannelHandler;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

@Component
public class MobileOtpChannelHandler implements OtpChannelHandler {

    private final SmsService smsService;

    public MobileOtpChannelHandler(SmsService smsService) {
        this.smsService = smsService;
    }

    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.SMS;
    }

    @Override
    public String normalizeReference(String reference) {
        return MobileNumberUtil.normalizeIndianMobileNumber(reference);
    }

    @Override
    public void dispatchOtp(String purpose, String reference, String otp) {
        dispatchOtp(purpose, reference, otp, null);
    }

    @Override
    public void dispatchOtp(String purpose, String reference, String otp, String otpReferenceId) {
        if (VerificationPurposes.LOGIN_AUTHENTICATION.equalsIgnoreCase(purpose)) {
            smsService.sendLoginOtp(null, reference, otp);
            return;
        }
        smsService.sendTemplateSms(
                null,
                reference,
                SmsTemplateCode.REGISTRATION_OTP,
                Map.of("otp", otp));
    }
}
