package com.maharecruitment.gov.in.web.service.verification.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;

@Service
public class NotificationServiceImpl implements OtpDispatchService, AccountNotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final Environment environment;

    public NotificationServiceImpl(
            JavaMailSender mailSender,
            RestClient restClient,
            Environment environment) {
        this.mailSender = mailSender;
        this.restClient = restClient;
        this.environment = environment;
    }

    @Override
    public void sendMobileOtp(String mobileNo, String otp) {
        if (!isEnabled("notification.sms.enabled", true)) {
            log.info("SMS dispatch is disabled. Skipping OTP SMS for mobile {}.", mobileNo);
            return;
        }
        String message = "Your MahaIT Recruitment OTP is " + otp + ". It is valid for 10 minutes.";
        sendSmsMessage(mobileNo, message, "OTP");
    }

    @Override
    public void sendEmailOtp(String email, String otp) {
        if (!isEnabled("notification.email.enabled", true)) {
            log.info("Email dispatch is disabled. Skipping OTP email for address {}.", email);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress());
        message.setTo(email);
        message.setSubject("MahaIT Recruitment Email Verification OTP");
        message.setText("Your email verification OTP is " + otp + ". It is valid for 10 minutes.");

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email verification OTP.", ex);
        }
    }

    @Override
    public void sendDepartmentCredentials(
            String email,
            String mobileNo,
            String contactName,
            String username,
            String temporaryPassword) {
        if (isEnabled("notification.email.enabled", true)) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(email);
            message.setSubject("MahaIT Recruitment Department Account Created");
            message.setText("""
                    Dear %s,

                    Your department registration has been submitted successfully and a department user account has been created.

                    Login ID: %s
                    Temporary Password: %s

                    Please sign in and change the password after first login.

                    Regards,
                    MahaIT Recruitment
                    """.formatted(contactName, username, temporaryPassword));

            try {
                mailSender.send(message);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to send department account credentials.", ex);
            }
        } else {
            log.info("Email dispatch is disabled. Skipping department credential email for {}.", email);
        }

        String smsMessage = "MahaIT Recruitment: Dept account created. Username: %s Password: %s. "
                .formatted(username, temporaryPassword)
                + "Please change password after first login.";
        if (isEnabled("notification.sms.enabled", true)) {
            sendSmsMessage(mobileNo, smsMessage, "department credentials");
        } else {
            log.info("SMS dispatch is disabled. Skipping department credential SMS for mobile {}.", mobileNo);
        }
    }

    @Override
    public void sendAgencyCredentials(String email, String contactName, String temporaryPassword) {
        if (!isEnabled("notification.email.enabled", true)) {
            log.info("Email dispatch is disabled. Skipping agency credential email for {}.", email);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress());
        message.setTo(email);
        message.setSubject("MahaIT Recruitment Agency Account Created");
        message.setText("""
                Dear %s,

                Your agency master record has been created successfully and an agency user account has been provisioned.

                Login ID: %s
                Temporary Password: %s

                Please sign in and change the password after first login.

                Regards,
                MahaIT Recruitment
                """.formatted(contactName, email, temporaryPassword));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send agency account credentials.", ex);
        }
    }

    private String getFromAddress() {
        String fromAddress = getProperty("spring.mail.from.email");
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = getProperty("spring.mail.username");
        }
        return fromAddress;
    }

    private void sendSmsMessage(String mobileNo, String message, String context) {
        String smsApiUrl = getProperty("sms.api.url");
        String smsApiKey = getProperty("sms.api.key");
        String senderId = getProperty("sms.sender-id");

        if (!StringUtils.hasText(mobileNo)) {
            log.warn("Mobile number is missing. Unable to dispatch {} SMS.", context);
            return;
        }

        if (!StringUtils.hasText(smsApiUrl) || !StringUtils.hasText(smsApiKey) || !StringUtils.hasText(senderId)) {
            log.warn("SMS gateway is not fully configured. Unable to dispatch {} SMS to {}.", context, mobileNo);
            return;
        }

        try {
            MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
            payload.add("apiKey", smsApiKey);
            payload.add("senderId", senderId);
            payload.add("mobile", mobileNo);
            payload.add("message", message);

            restClient.post()
                    .uri(smsApiUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to dispatch " + context + " SMS.", ex);
        }
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        String value = environment.getProperty(key);
        if (!StringUtils.hasText(value) || value.contains("${")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private String getProperty(String key) {
        String value = environment.getProperty(key, "");
        if (value != null && value.contains("${")) {
            return "";
        }
        return value == null ? "" : value.trim();
    }
}
