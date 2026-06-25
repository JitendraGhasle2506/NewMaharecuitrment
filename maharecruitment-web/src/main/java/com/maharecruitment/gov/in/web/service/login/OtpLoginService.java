package com.maharecruitment.gov.in.web.service.login;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.CustomUserDetailsService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationResult;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpSession;

@Service
public class OtpLoginService {

    private final CustomUserDetailsService userDetailsService;
    private final UserAffiliationService userAffiliationService;
    private final OtpVerificationService otpVerificationService;
    private final NotificationChannelProperties notificationChannelProperties;

    public OtpLoginService(
            CustomUserDetailsService userDetailsService,
            UserAffiliationService userAffiliationService,
            OtpVerificationService otpVerificationService,
            NotificationChannelProperties notificationChannelProperties) {
        this.userDetailsService = userDetailsService;
        this.userAffiliationService = userAffiliationService;
        this.otpVerificationService = otpVerificationService;
        this.notificationChannelProperties = notificationChannelProperties;
    }

    public OtpVerificationResult sendOtp(
            HttpSession session,
            String identifier,
            VerificationChannel channel,
            OtpRequestContext context) {
        ensureChannelEnabled(channel);
        User user;
        try {
            user = userDetailsService.loadDomainUserByIdentifier(identifier);
        } catch (RuntimeException ex) {
            otpVerificationService.recordUnknownSendAttempt(
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    channel,
                    identifier,
                    context);
            return OtpVerificationResult.sent(0, 0);
        }

        String reference = resolveReference(user, channel);
        return otpVerificationService.sendOtp(
                session,
                VerificationPurposes.LOGIN_AUTHENTICATION,
                channel,
                reference,
                context);
    }

    public boolean isChannelEnabled(VerificationChannel channel) {
        if (channel == null) {
            return false;
        }

        return channel == VerificationChannel.EMAIL
                ? notificationChannelProperties.isEmailEnabled()
                : notificationChannelProperties.isSmsEnabled();
    }

    public String disabledChannelMessage(VerificationChannel channel) {
        String channelLabel = channel == VerificationChannel.MOBILE ? "Mobile" : "Email";
        return channelLabel + " OTP login is not enabled in this environment.";
    }

    public Authentication authenticate(
            HttpSession session,
            String identifier,
            VerificationChannel channel,
            String otp,
            String captchaId,
            String captchaAnswer,
            OtpRequestContext context) {
        ensureChannelEnabled(channel);
        User user;
        try {
            user = userDetailsService.loadDomainUserByIdentifier(identifier);
        } catch (RuntimeException ex) {
            otpVerificationService.recordUnknownVerifyAttempt(
                    VerificationPurposes.LOGIN_AUTHENTICATION,
                    channel,
                    identifier,
                    context);
            throw ex;
        }
        String reference = resolveReference(user, channel);
        otpVerificationService.verifyOtp(
                session,
                VerificationPurposes.LOGIN_AUTHENTICATION,
                channel,
                reference,
                otp,
                captchaId,
                captchaAnswer,
                context);
        otpVerificationService.clear(session, VerificationPurposes.LOGIN_AUTHENTICATION);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities());
    }

    private String resolveReference(User user, VerificationChannel channel) {
        if (channel == VerificationChannel.EMAIL) {
            return user.getEmail();
        }

        String mobileNo = userAffiliationService.getAffiliation(user).getMobileNo();
        if (!StringUtils.hasText(mobileNo)) {
            throw new IllegalStateException("No mobile number is registered for this user.");
        }
        return mobileNo.trim();
    }

    private void ensureChannelEnabled(VerificationChannel channel) {
        if (!isChannelEnabled(channel)) {
            throw new IllegalStateException(disabledChannelMessage(channel));
        }
    }
}
