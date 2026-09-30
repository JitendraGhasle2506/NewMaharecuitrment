package com.maharecruitment.gov.in.web.service.notification;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.web.entity.notification.SmtpConfigurationEntity;
import com.maharecruitment.gov.in.web.repository.notification.SmtpConfigurationRepository;

@Service
public class SmtpCredentialService {

    private final SmtpConfigurationRepository repository;

    public SmtpCredentialService(SmtpConfigurationRepository repository) {
        this.repository = repository;
    }

    public SmtpCredentials getRequiredCredentials() {
        SmtpConfigurationEntity configuration = repository
                .findById(SmtpConfigurationEntity.ACTIVE_CONFIGURATION_ID)
                .filter(SmtpConfigurationEntity::isEnabled)
                .orElseThrow(() -> new IllegalStateException(
                        "Active SMTP credentials are not configured in the smtp_configuration database table."));

        if (!StringUtils.hasText(configuration.getUsername())
                || !StringUtils.hasText(configuration.getPassword())) {
            throw new IllegalStateException(
                    "SMTP username and password must be configured in the smtp_configuration database table.");
        }

        return new SmtpCredentials(configuration.getUsername().trim(), configuration.getPassword());
    }

    public record SmtpCredentials(String username, String password) {
    }
}
