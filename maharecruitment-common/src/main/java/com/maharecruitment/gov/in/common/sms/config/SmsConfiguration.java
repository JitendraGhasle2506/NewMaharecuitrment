package com.maharecruitment.gov.in.common.sms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.maharecruitment.gov.in.common.sms.template.SmsTemplateProperties;

@Configuration
@EnableConfigurationProperties({
        AclSmsProperties.class,
        SmsTemplateProperties.class
})
public class SmsConfiguration {
}
