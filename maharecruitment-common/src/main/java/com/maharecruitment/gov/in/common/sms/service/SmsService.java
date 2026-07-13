package com.maharecruitment.gov.in.common.sms.service;

import java.util.Map;

import com.maharecruitment.gov.in.common.sms.template.SmsTemplateCode;

public interface SmsService {

    void sendLoginOtp(Long userId, String mobileNumber, String otp);

    void sendTemplateSms(
            Long userId,
            String mobileNumber,
            SmsTemplateCode templateCode,
            Map<String, String> parameters);
}
