package com.maharecruitment.gov.in.common.sms.template;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maharecruitment.sms")
public class SmsTemplateProperties {

    private Map<String, Template> templates = new LinkedHashMap<>();

    public Map<String, Template> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, Template> templates) {
        this.templates = templates == null ? new LinkedHashMap<>() : new LinkedHashMap<>(templates);
    }

    public static class Template {

        private String appId;

        private String message;

        private Set<String> requiredParameters = new LinkedHashSet<>();

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Set<String> getRequiredParameters() {
            return requiredParameters;
        }

        public void setRequiredParameters(Set<String> requiredParameters) {
            this.requiredParameters = requiredParameters == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(requiredParameters);
        }
    }
}
