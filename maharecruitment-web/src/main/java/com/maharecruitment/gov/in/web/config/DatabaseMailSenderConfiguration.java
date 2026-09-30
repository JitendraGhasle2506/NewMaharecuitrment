package com.maharecruitment.gov.in.web.config;

import java.io.InputStream;
import java.util.Properties;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import com.maharecruitment.gov.in.web.service.notification.SmtpCredentialService;
import com.maharecruitment.gov.in.web.service.notification.SmtpCredentialService.SmtpCredentials;

import jakarta.mail.internet.MimeMessage;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailProperties.class)
public class DatabaseMailSenderConfiguration {

    @Bean
    JavaMailSender databaseBackedMailSender(
            MailProperties properties,
            SmtpCredentialService credentialService) {
        return new DatabaseBackedMailSender(properties, credentialService);
    }

    private static final class DatabaseBackedMailSender implements JavaMailSender {

        private final MailProperties properties;
        private final SmtpCredentialService credentialService;

        private DatabaseBackedMailSender(MailProperties properties, SmtpCredentialService credentialService) {
            this.properties = properties;
            this.credentialService = credentialService;
        }

        @Override
        public MimeMessage createMimeMessage() {
            return newSender(false).createMimeMessage();
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
            return newSender(false).createMimeMessage(contentStream);
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            newSender(true).send(mimeMessages);
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            newSender(true).send(mimeMessagePreparators);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            newSender(true).send(simpleMessages);
        }

        private JavaMailSenderImpl newSender(boolean credentialsRequired) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(properties.getHost());
            if (properties.getPort() != null) {
                sender.setPort(properties.getPort());
            }
            sender.setProtocol(properties.getProtocol());
            sender.setDefaultEncoding(properties.getDefaultEncoding().name());
            Properties javaMailProperties = new Properties();
            javaMailProperties.putAll(properties.getProperties());
            sender.setJavaMailProperties(javaMailProperties);

            if (credentialsRequired) {
                SmtpCredentials credentials = credentialService.getRequiredCredentials();
                sender.setUsername(credentials.username());
                sender.setPassword(credentials.password());
            }
            return sender;
        }
    }
}
