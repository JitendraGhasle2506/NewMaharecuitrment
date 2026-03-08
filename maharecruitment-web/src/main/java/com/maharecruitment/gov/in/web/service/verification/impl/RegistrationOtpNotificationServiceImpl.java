package com.maharecruitment.gov.in.web.service.verification.impl;

import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.maharecruitment.gov.in.web.service.verification.RegistrationOtpNotificationService;

@Service
public class RegistrationOtpNotificationServiceImpl implements RegistrationOtpNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationOtpNotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final Environment environment;

    public RegistrationOtpNotificationServiceImpl(
            JavaMailSender mailSender,
            RestClient restClient,
            Environment environment) {
        this.mailSender = mailSender;
        this.restClient = restClient;
        this.environment = environment;
    }

    @Override
    public void sendMobileOtp(String mobileNo, String otp) {
        String message = "Your MahaIT Recruitment OTP is " + otp + ". It is valid for 10 minutes.";
        String smsApiUrl = getProperty("sms.api.url");
        String smsApiKey = getProperty("sms.api.key");
        String senderId = getProperty("sms.sender-id");

        log.info("Generated mobile OTP for {} is {}", mobileNo, otp);

        if (!StringUtils.hasText(smsApiUrl) || !StringUtils.hasText(smsApiKey) || !StringUtils.hasText(senderId)) {
            log.warn("SMS gateway is not fully configured. OTP was generated but not dispatched to mobile {}.", mobileNo);
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
            throw new IllegalStateException("Failed to dispatch mobile OTP.", ex);
        }
    }

    @Override
    public void sendEmailOtp(String email, String otp) {
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
    public void sendDepartmentCredentials(String email, String contactName, String temporaryPassword) {
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
                """.formatted(contactName, email, temporaryPassword));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send department account credentials.", ex);
        }
    }

    private String getFromAddress() {
        String fromAddress = getProperty("spring.mail.from.email");
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = getProperty("spring.mail.username");
        }
        return fromAddress;
    }

    private String getProperty(String key) {
        String value = environment.getProperty(key, "");
        if (value != null && value.contains("${")) {
            return "";
        }
        return value == null ? "" : value.trim();
    }
}
