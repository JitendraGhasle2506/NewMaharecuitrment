package com.maharecruitment.gov.in.common.sms.template;

import java.util.Set;

public record SmsTemplateDefinition(
        SmsTemplateCode code,
        String appId,
        String message,
        Set<String> requiredParameters
) {
}
