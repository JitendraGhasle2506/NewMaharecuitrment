package com.maharecruitment.gov.in.web.service.login;

import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.CustomUserDetailsService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpSession;

@Service
public class OtpLoginService {

    private final CustomUserDetailsService userDetailsService;
    private final UserAffiliationService userAffiliationService;
    private final OtpVerificationService otpVerificationService;
    private final Environment environment;

    public OtpLoginService(
            CustomUserDetailsService userDetailsService,
            UserAffiliationService userAffiliationService,
            OtpVerificationService otpVerificationService,
            Environment environment) {
        this.userDetailsService = userDetailsService;
        this.userAffiliationService = userAffiliationService;
        this.otpVerificationService = otpVerificationService;
        this.environment = environment;
    }

    public void sendOtp(HttpSession session, String identifier, VerificationChannel channel) {
        ensureChannelEnabled(channel);
        User user = userDetailsService.loadDomainUserByIdentifier(identifier);
        String reference = resolveReference(user, channel);
        otpVerificationService.clear(session, VerificationPurposes.LOGIN_AUTHENTICATION);
        otpVerificationService.sendOtp(session, VerificationPurposes.LOGIN_AUTHENTICATION, channel, reference);
    }

    public Authentication authenticate(HttpSession session, String identifier, VerificationChannel channel, String otp) {
        ensureChannelEnabled(channel);
        User user = userDetailsService.loadDomainUserByIdentifier(identifier);
        String reference = resolveReference(user, channel);
        otpVerificationService.verifyOtp(session, VerificationPurposes.LOGIN_AUTHENTICATION, channel, reference, otp);
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
        String propertyKey = channel == VerificationChannel.EMAIL
                ? "notification.email.enabled"
                : "notification.sms.enabled";

        if (!isEnabled(propertyKey, true)) {
            String channelLabel = channel == VerificationChannel.EMAIL ? "Email" : "Mobile";
            throw new IllegalStateException(channelLabel + " OTP login is not enabled in this environment.");
        }
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        String value = environment.getProperty(key);
        if (!StringUtils.hasText(value) || value.contains("${")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
