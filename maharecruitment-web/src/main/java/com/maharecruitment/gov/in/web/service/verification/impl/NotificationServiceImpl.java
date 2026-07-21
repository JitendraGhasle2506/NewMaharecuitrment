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

import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;
import com.maharecruitment.gov.in.web.util.ApplicationUrlService;

@Service
public class NotificationServiceImpl implements OtpDispatchService, AccountNotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final Environment environment;
    private final NotificationChannelProperties notificationChannelProperties;
    private final ApplicationUrlService applicationUrlService;

    public NotificationServiceImpl(
            JavaMailSender mailSender,
            RestClient restClient,
            Environment environment,
            NotificationChannelProperties notificationChannelProperties,
            ApplicationUrlService applicationUrlService) {
        this.mailSender = mailSender;
        this.restClient = restClient;
        this.environment = environment;
        this.notificationChannelProperties = notificationChannelProperties;
        this.applicationUrlService = applicationUrlService;
    }

    @Override
    public void sendMobileOtp(String mobileNo, String otp) {
        sendMobileOtp(mobileNo, otp, null);
    }

    @Override
    public void sendMobileOtp(String mobileNo, String otp, String otpReferenceId) {
        if (!notificationChannelProperties.isSmsEnabled()) {
            log.info("SMS dispatch is disabled. Skipping OTP SMS for mobile {}.", mobileNo);
            return;
        }
        String message = "Your MahaIT Recruitment OTP is " + otp
                + ". OTP ID: " + formatOtpReferenceId(otpReferenceId)
                + ". It is valid for " + resolveOtpValidityText() + ".";
        sendSmsMessage(mobileNo, message, "OTP");
    }

    @Override
    public void sendEmailOtp(String email, String otp) {
        sendEmailOtp(email, otp, null);
    }

    @Override
    public void sendEmailOtp(String email, String otp, String purpose) {
        sendEmailOtp(email, otp, purpose, null);
    }

    @Override
    public void sendEmailOtp(String email, String otp, String purpose, String otpReferenceId) {
        if (!notificationChannelProperties.isEmailEnabled()) {
            log.info("Email dispatch is disabled. Skipping OTP email for address {}.", email);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress());
        message.setTo(email);
        if (VerificationPurposes.LOGIN_AUTHENTICATION.equalsIgnoreCase(valueOrBlank(purpose))) {
            message.setSubject("Maha Recruitment Portal Login OTP");
            message.setText(buildLoginOtpEmailBody(otp, otpReferenceId));
        } else if (VerificationPurposes.PASSWORD_RESET.equalsIgnoreCase(valueOrBlank(purpose))) {
            message.setSubject("MahaIT Recruitment Password Reset OTP");
            message.setText(buildPasswordResetOtpEmailBody(otp, otpReferenceId));
        } else if (VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT.equalsIgnoreCase(valueOrBlank(purpose))) {
            message.setSubject("MahaIT Recruitment Department Registration Email Verification OTP");
            message.setText(buildDepartmentRegistrationOtpEmailBody(otp, email, otpReferenceId));
        } else {
            message.setSubject("MahaIT Recruitment Email Verification OTP");
            message.setText(buildGenericVerificationOtpEmailBody(otp, otpReferenceId));
        }

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email verification OTP to {} from {}. Reason: {}",
                    email, message.getFrom(), extractFailureReason(ex), ex);
            throw new IllegalStateException(buildFailureMessage("Failed to send email verification OTP.", ex), ex);
        }
    }

    @Override
    public void sendDepartmentCredentials(
            String email,
            String mobileNo,
            String contactName,
            String username,
            String temporaryPassword) {
        if (notificationChannelProperties.isEmailEnabled()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(email);
            message.setSubject("MahaIT Recruitment Department Account Created");
            message.setText(
                    """
                            Dear %s,

                            Your department registration has been submitted successfully and a department user account has been created.

                            Username: %s
                            Temporary Password: %s

                            Please sign in at %s and change the password after first login.

                            Regards,
                            MahaIT Recruitment
                            """
                            .formatted(contactName, username, temporaryPassword, portalLoginUrl()));

            try {
                mailSender.send(message);
            } catch (Exception ex) {
                log.error("Failed to send department account credentials to {} from {}. Reason: {}",
                        email, message.getFrom(), extractFailureReason(ex), ex);
                throw new IllegalStateException(
                        buildFailureMessage("Failed to send department account credentials.", ex), ex);
            }
        } else {
            log.info("Email dispatch is disabled. Skipping department credential email for {}.", email);
        }

        String smsMessage = "MahaIT Recruitment: Dept account created. Username: %s Password: %s. "
                .formatted(username, temporaryPassword)
                + "Please change password after first login.";
        if (notificationChannelProperties.isSmsEnabled()) {
            sendSmsMessage(mobileNo, smsMessage, "department credentials");
        } else {
            log.info("SMS dispatch is disabled. Skipping department credential SMS for mobile {}.", mobileNo);
        }
    }

    @Override
    public void sendEmployeeCredentials(
            String email,
            String mobileNo,
            String contactName,
            String username,
            String temporaryPassword) {
        Exception emailFailure = null;
        Exception smsFailure = null;

        if (notificationChannelProperties.isEmailEnabled()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(email);
            message.setSubject("MahaIT Recruitment Employee Account Created");
            message.setText("""
                    Dear %s,

                    Your employee account has been created successfully during HR onboarding.

                    Username: %s
                    Temporary Password: %s

                    Please sign in at %s and change the password after first login.

                    Regards,
                    MahaIT Recruitment
                    """.formatted(contactName, username, temporaryPassword, portalLoginUrl()));

            try {
                mailSender.send(message);
            } catch (Exception ex) {
                emailFailure = ex;
                log.warn("Failed to send employee credential email to {} from {}. Reason: {}",
                        email, message.getFrom(), extractFailureReason(ex), ex);
            }
        } else {
            log.info("Email dispatch is disabled. Skipping employee credential email for {}.", email);
        }

        String smsMessage = "MahaIT Recruitment: Employee account created. Username: %s Password: %s. "
                .formatted(username, temporaryPassword)
                + "Please change password after first login.";
        if (notificationChannelProperties.isSmsEnabled()) {
            try {
                sendSmsMessage(mobileNo, smsMessage, "employee credentials");
            } catch (Exception ex) {
                smsFailure = ex;
                log.warn("Failed to send employee credential SMS to {}.", mobileNo, ex);
            }
        } else {
            log.info("SMS dispatch is disabled. Skipping employee credential SMS for mobile {}.", mobileNo);
        }

        if (emailFailure != null || smsFailure != null) {
            IllegalStateException failure = new IllegalStateException(
                    "Failed to deliver employee account credentials through all configured channels.");
            if (emailFailure != null) {
                failure.addSuppressed(emailFailure);
            }
            if (smsFailure != null) {
                failure.addSuppressed(smsFailure);
            }
            throw failure;
        }
    }

    @Override
    public void sendAgencyCredentials(String email, String contactName, String temporaryPassword) {
        if (!notificationChannelProperties.isEmailEnabled()) {
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

                Username: %s
                Temporary Password: %s

                Please sign in at %s and change the password after first login.

                Regards,
                MahaIT Recruitment
                """.formatted(contactName, email, temporaryPassword, portalLoginUrl()));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send agency account credentials to {} from {}. Reason: {}",
                    email, message.getFrom(), extractFailureReason(ex), ex);
            throw new IllegalStateException(buildFailureMessage("Failed to send agency account credentials.", ex), ex);
        }
    }

    @Override
    public void sendResignationNotification(String email, String role, String employeeName, String resignDate) {
        if (!notificationChannelProperties.isEmailEnabled()) {
            log.info("Email dispatch is disabled. Skipping resignation notification email for {}.", email);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress());
        message.setTo(email);
        message.setSubject("Resignation Submitted by " + employeeName);
        message.setText("""
                Dear %s,

                This is to notify you that %s has submitted their resignation.
                Resignation Date: %s

                Please log in to the MahaIT Recruitment portal to process the relieving request:
                %s

                Regards,
                MahaIT Recruitment
                """.formatted(role, employeeName, resignDate, portalLoginUrl()));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send resignation notification to {} from {}. Reason: {}",
                    email, message.getFrom(), extractFailureReason(ex), ex);
        }
    }

    @Override
    public void sendPasswordResetCompleted(String email, String mobileNo, String name) {
        if (notificationChannelProperties.isEmailEnabled() && StringUtils.hasText(email)) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(email);
            message.setSubject("MahaIT Recruitment Password Changed");
            message.setText("""
                    Dear %s,

                    Your MahaIT Recruitment account password was changed successfully.

                    If you did not perform this action, please contact administrator support immediately.

                    Regards,
                    MahaIT Recruitment
                    """.formatted(StringUtils.hasText(name) ? name.trim() : "User"));

            try {
                mailSender.send(message);
            } catch (Exception ex) {
                log.warn("Failed to send password reset notification email to {} from {}. Reason: {}",
                        email, message.getFrom(), extractFailureReason(ex), ex);
            }
        }

        if (notificationChannelProperties.isSmsEnabled() && StringUtils.hasText(mobileNo)) {
            try {
                sendSmsMessage(
                        mobileNo,
                        "MahaIT Recruitment: Your account password was changed successfully. If this was not you, contact support.",
                        "password reset notification");
            } catch (Exception ex) {
                log.warn("Failed to send password reset notification SMS to {}.", mobileNo, ex);
            }
        }
    }

    private String buildFailureMessage(String baseMessage, Exception ex) {
        String reason = extractFailureReason(ex);
        if (!StringUtils.hasText(reason) || baseMessage.contains(reason)) {
            return baseMessage;
        }
        return baseMessage + " " + reason;
    }

    private String buildLoginOtpEmailBody(String otp, String otpReferenceId) {
        return """
                Dear User,

                Your One-Time Password (OTP) for login to the Maha Recruitment Portal is:

                %s

                OTP ID: %s

                This OTP is valid for %s. Please do not share this OTP with anyone for security reasons.

                If you did not request this OTP, please ignore this email.

                This is an automated message. Please do not reply to this email.

                Regards,
                Maha Recruitment Team
                """.formatted(otp, formatOtpReferenceId(otpReferenceId), resolveOtpValidityText());
    }

    private String buildPasswordResetOtpEmailBody(String otp, String otpReferenceId) {
        return """
                Dear User,

                Your One-Time Password (OTP) to reset your MahaIT Recruitment password is:

                %s

                OTP ID: %s

                This OTP is valid for %s. Please do not share this OTP with anyone.

                If you did not request a password reset, please ignore this email.

                Regards,
                Maha Recruitment Team
                """.formatted(otp, formatOtpReferenceId(otpReferenceId), resolveOtpValidityText());
    }

    private String buildDepartmentRegistrationOtpEmailBody(String otp, String email, String otpReferenceId) {
        return """
                Dear User,

                We received a request to verify %s for department registration on MahaIT Recruitment.

                Your One-Time Password (OTP) is:

                %s

                OTP ID: %s

                This OTP is valid for %s. Do not share this OTP with anyone.

                If you did not initiate this request, please ignore this email.

                This is an automated message. Please do not reply to this email.

                Regards,
                Maha Recruitment Team
                """.formatted(email, otp, formatOtpReferenceId(otpReferenceId), resolveOtpValidityText());
    }

    private String buildGenericVerificationOtpEmailBody(String otp, String otpReferenceId) {
        return """
                Dear User,

                Your email verification OTP is:

                %s

                OTP ID: %s

                This OTP is valid for %s. Do not share this OTP with anyone.

                If you did not request this OTP, please ignore this email.

                Regards,
                Maha Recruitment Team
                """.formatted(otp, formatOtpReferenceId(otpReferenceId), resolveOtpValidityText());
    }

    private String formatOtpReferenceId(String otpReferenceId) {
        return StringUtils.hasText(otpReferenceId) ? otpReferenceId.trim() : "N/A";
    }

    private String extractFailureReason(Throwable throwable) {
        Throwable current = throwable;
        String fallback = "Unexpected mail transport error.";
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                fallback = current.getMessage().trim();
            }
            current = current.getCause();
        }
        return fallback;
    }

    private String resolveOtpValidityText() {
        int expiryMinutes = environment.getProperty("otp.expiry-minutes", Integer.class, 5);
        int expirySeconds = Math.max(1, expiryMinutes) * 60;
        if (expirySeconds % 60 == 0) {
            int minutes = expirySeconds / 60;
            return minutes == 1 ? "1 minute" : minutes + " minutes";
        }
        return expirySeconds == 1 ? "1 second" : expirySeconds + " seconds";
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private String portalLoginUrl() {
        return applicationUrlService.absoluteUrl("/login");
    }

    private String getFromAddress() {
        String fromAddress = getProperty("spring.mail.from.email");
        if (StringUtils.hasText(fromAddress)) {
            return fromAddress;
        }

        String username = getProperty("spring.mail.username");
        if (StringUtils.hasText(username) && username.contains("@")) {
            return username;
        }

        throw new IllegalStateException(
                "Email sender address is not configured. Set spring.mail.from.email or SMTP_FROM_EMAIL to a verified sender address.");
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

    private String getProperty(String key) {
        String value = environment.getProperty(key, "");
        if (value != null && value.contains("${")) {
            return "";
        }
        return value == null ? "" : value.trim();
    }
}
