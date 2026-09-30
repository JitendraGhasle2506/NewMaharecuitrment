package com.maharecruitment.gov.in.web.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import com.maharecruitment.gov.in.web.service.notification.EmailDispatchLogService;
import com.maharecruitment.gov.in.web.service.notification.SmtpCredentialService;
import com.maharecruitment.gov.in.web.service.notification.SmtpCredentialService.SmtpCredentials;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailProperties.class)
public class DatabaseMailSenderConfiguration {

    @Bean
    JavaMailSender databaseBackedMailSender(
            MailProperties properties,
            SmtpCredentialService credentialService,
            EmailDispatchLogService emailDispatchLogService) {
        return new DatabaseBackedMailSender(properties, credentialService, emailDispatchLogService);
    }

    private static final class DatabaseBackedMailSender implements JavaMailSender {

        private static final Logger log = LoggerFactory.getLogger(DatabaseBackedMailSender.class);

        private final MailProperties properties;
        private final SmtpCredentialService credentialService;
        private final EmailDispatchLogService emailDispatchLogService;

        private DatabaseBackedMailSender(
                MailProperties properties,
                SmtpCredentialService credentialService,
                EmailDispatchLogService emailDispatchLogService) {
            this.properties = properties;
            this.credentialService = credentialService;
            this.emailDispatchLogService = emailDispatchLogService;
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
            List<Long> logIds = createMimeMessageLogs(mimeMessages);
            try {
                newSender(true).send(mimeMessages);
                emailDispatchLogService.markSent(logIds);
            } catch (RuntimeException failure) {
                markFailed(logIds, failure);
                throw failure;
            }
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            MimeMessage[] messages = new MimeMessage[mimeMessagePreparators.length];
            try {
                for (int index = 0; index < mimeMessagePreparators.length; index++) {
                    messages[index] = createMimeMessage();
                    mimeMessagePreparators[index].prepare(messages[index]);
                }
            } catch (Exception failure) {
                throw new MailPreparationException(failure);
            }
            send(messages);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            List<Long> logIds = createSimpleMessageLogs(simpleMessages);
            try {
                newSender(true).send(simpleMessages);
                emailDispatchLogService.markSent(logIds);
            } catch (RuntimeException failure) {
                markFailed(logIds, failure);
                throw failure;
            }
        }

        private List<Long> createSimpleMessageLogs(SimpleMailMessage[] messages) {
            List<Long> logIds = new ArrayList<>();
            for (SimpleMailMessage message : messages) {
                logIds.addAll(emailDispatchLogService.createPending(
                        message.getFrom(), allRecipients(message), message.getSubject()));
            }
            return logIds;
        }

        private List<Long> createMimeMessageLogs(MimeMessage[] messages) {
            List<Long> logIds = new ArrayList<>();
            for (MimeMessage message : messages) {
                try {
                    logIds.addAll(emailDispatchLogService.createPending(
                            firstAddress(message.getFrom()),
                            addressStrings(message.getAllRecipients()),
                            message.getSubject()));
                } catch (MessagingException failure) {
                    throw new MailPreparationException("Unable to read email audit details.", failure);
                }
            }
            return logIds;
        }

        private String[] allRecipients(SimpleMailMessage message) {
            List<String> recipients = new ArrayList<>();
            addRecipients(recipients, message.getTo());
            addRecipients(recipients, message.getCc());
            addRecipients(recipients, message.getBcc());
            return recipients.toArray(String[]::new);
        }

        private void addRecipients(List<String> recipients, String[] addresses) {
            if (addresses != null) {
                recipients.addAll(Arrays.asList(addresses));
            }
        }

        private String firstAddress(Address[] addresses) {
            return addresses == null || addresses.length == 0 ? null : addresses[0].toString();
        }

        private String[] addressStrings(Address[] addresses) {
            return addresses == null
                    ? new String[0]
                    : Arrays.stream(addresses).map(Address::toString).toArray(String[]::new);
        }

        private void markFailed(List<Long> logIds, RuntimeException deliveryFailure) {
            try {
                emailDispatchLogService.markFailed(logIds, deliveryFailure);
            } catch (RuntimeException loggingFailure) {
                deliveryFailure.addSuppressed(loggingFailure);
                log.error("Unable to update failed email audit records.", loggingFailure);
            }
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
