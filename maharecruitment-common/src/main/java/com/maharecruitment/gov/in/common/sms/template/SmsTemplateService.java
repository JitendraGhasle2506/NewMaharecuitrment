package com.maharecruitment.gov.in.common.sms.template;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsTemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)\\}");

    private static final String LOGIN_OTP_TEMPLATE =
            "OTP for Maharecruitment Portal login: %s. "
                    + "Valid for 10 minutes. "
                    + "Do not share this OTP with anyone. - MAHGOV";

    private final SmsTemplateProperties properties;

    public SmsTemplateService(SmsTemplateProperties properties) {
        this.properties = properties;
    }

    public String buildLoginOtpMessage(String otp) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must contain exactly six numeric digits");
        }

        return LOGIN_OTP_TEMPLATE.formatted(otp);
    }

    public SmsTemplateDefinition getTemplateDefinition(SmsTemplateCode templateCode) {
        if (templateCode == null) {
            throw new IllegalArgumentException("SMS template code is required");
        }

        String key = toPropertyKey(templateCode);
        SmsTemplateProperties.Template template = properties.getTemplates().get(key);
        if (template == null || !StringUtils.hasText(template.getAppId())
                || !StringUtils.hasText(template.getMessage())) {
            throw new IllegalArgumentException("SMS template is not configured: " + templateCode.name());
        }

        Set<String> placeholders = extractPlaceholders(template.getMessage());
        Set<String> requiredParameters = template.getRequiredParameters().isEmpty()
                ? placeholders
                : normalizeParameterNames(template.getRequiredParameters());

        if (!placeholders.containsAll(requiredParameters)) {
            throw new IllegalArgumentException("SMS template required parameters do not match placeholders");
        }

        return new SmsTemplateDefinition(
                templateCode,
                template.getAppId().trim(),
                template.getMessage(),
                Set.copyOf(requiredParameters));
    }

    public String buildTemplateMessage(SmsTemplateCode templateCode, Map<String, String> parameters) {
        SmsTemplateDefinition definition = getTemplateDefinition(templateCode);
        Map<String, String> normalizedParameters = normalizeParameters(parameters);
        Set<String> placeholders = extractPlaceholders(definition.message());

        Set<String> missingParameters = new LinkedHashSet<>(definition.requiredParameters());
        missingParameters.removeAll(normalizedParameters.keySet());
        if (!missingParameters.isEmpty()) {
            throw new IllegalArgumentException("Missing SMS template parameters: " + missingParameters);
        }

        Set<String> extraParameters = new LinkedHashSet<>(normalizedParameters.keySet());
        extraParameters.removeAll(placeholders);
        if (!extraParameters.isEmpty()) {
            throw new IllegalArgumentException("Unexpected SMS template parameters: " + extraParameters);
        }

        String message = definition.message();
        for (String placeholder : placeholders) {
            String value = normalizedParameters.get(placeholder);
            if (!StringUtils.hasText(value)) {
                throw new IllegalArgumentException("Missing SMS template parameter: " + placeholder);
            }
            message = message.replace("{" + placeholder + "}", value);
        }

        return message;
    }

    private Map<String, String> normalizeParameters(Map<String, String> parameters) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (parameters == null) {
            return normalized;
        }

        parameters.forEach((name, value) -> {
            if (!StringUtils.hasText(name)) {
                throw new IllegalArgumentException("SMS template parameter name is required");
            }
            normalized.put(name.trim().toLowerCase(Locale.ROOT), value == null ? "" : value.trim());
        });
        return normalized;
    }

    private Set<String> normalizeParameterNames(Set<String> parameterNames) {
        Set<String> normalized = new LinkedHashSet<>();
        parameterNames.forEach(name -> {
            if (StringUtils.hasText(name)) {
                normalized.add(name.trim().toLowerCase(Locale.ROOT));
            }
        });
        return normalized;
    }

    private Set<String> extractPlaceholders(String message) {
        Set<String> placeholders = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        while (matcher.find()) {
            placeholders.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return placeholders;
    }

    private String toPropertyKey(SmsTemplateCode templateCode) {
        return templateCode.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
