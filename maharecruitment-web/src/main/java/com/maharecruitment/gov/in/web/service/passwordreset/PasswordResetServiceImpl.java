package com.maharecruitment.gov.in.web.service.passwordreset;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.AgencyAccountAccessService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetOtpVerifyRequest;
import com.maharecruitment.gov.in.web.dto.passwordreset.PasswordResetResponse;
import com.maharecruitment.gov.in.web.dto.passwordreset.ResetPasswordRequest;
import com.maharecruitment.gov.in.web.entity.passwordreset.PasswordResetRequestEntity;
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.PasswordResetProperties;
import com.maharecruitment.gov.in.web.repository.passwordreset.PasswordResetRequestRepository;
import com.maharecruitment.gov.in.web.service.mobile.MobileRefreshTokenService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;
import com.maharecruitment.gov.in.web.service.verification.OtpDispatchService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final Pattern DIGITS = Pattern.compile("^\\d{10,15}$");
    private static final String GENERIC_OTP_MESSAGE =
            "If the account information is valid, an OTP has been sent to the registered contact details.";
    private static final String OTP_VERIFIED_MESSAGE =
            "OTP verified successfully. Use the reset token to set a new password.";
    private static final String RESET_COMPLETED_MESSAGE = "Password reset successfully.";
    private static final List<PasswordResetStatus> ACTIVE_STATUSES =
            List.of(PasswordResetStatus.OTP_SENT, PasswordResetStatus.OTP_VERIFIED);

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAffiliationService userAffiliationService;
    private final AgencyAccountAccessService agencyAccountAccessService;
    private final PasswordResetRequestRepository resetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureOtpGenerator otpGenerator;
    private final SecureResetTokenGenerator tokenGenerator;
    private final PasswordResetTokenHasher tokenHasher;
    private final PasswordResetRateLimiter rateLimiter;
    private final PasswordResetProperties properties;
    private final NotificationChannelProperties notificationChannelProperties;
    private final OtpDispatchService otpDispatchService;
    private final AccountNotificationService accountNotificationService;
    private final MobileRefreshTokenService mobileRefreshTokenService;
    private final PasswordResetAuditService auditService;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            UserAffiliationService userAffiliationService,
            AgencyAccountAccessService agencyAccountAccessService,
            PasswordResetRequestRepository resetRequestRepository,
            PasswordEncoder passwordEncoder,
            SecureOtpGenerator otpGenerator,
            SecureResetTokenGenerator tokenGenerator,
            PasswordResetTokenHasher tokenHasher,
            PasswordResetRateLimiter rateLimiter,
            PasswordResetProperties properties,
            NotificationChannelProperties notificationChannelProperties,
            OtpDispatchService otpDispatchService,
            AccountNotificationService accountNotificationService,
            MobileRefreshTokenService mobileRefreshTokenService,
            PasswordResetAuditService auditService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.userAffiliationService = userAffiliationService;
        this.agencyAccountAccessService = agencyAccountAccessService;
        this.resetRequestRepository = resetRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpGenerator = otpGenerator;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.notificationChannelProperties = notificationChannelProperties;
        this.otpDispatchService = otpDispatchService;
        this.accountNotificationService = accountNotificationService;
        this.mobileRefreshTokenService = mobileRefreshTokenService;
        this.auditService = auditService;
    }

    @Override
    public PasswordResetResponse requestOtp(
            PasswordResetOtpRequest request,
            ResetPasswordChannel channel,
            String clientIp,
            String userAgent) {
        String normalizedIdentifier = normalizeIdentifier(request == null ? null : request.getIdentifier());
        ResetPasswordChannel resetChannel = requireChannel(channel);
        String normalizedIp = truncate(clientIp, 100);

        Optional<User> userCandidate = resolveEligibleUser(normalizedIdentifier);
        rateLimiter.checkOtpRequestAllowed(
                normalizedIdentifier,
                normalizedIp,
                userCandidate.map(User::getId).orElse(null));
        if (userCandidate.isEmpty()) {
            auditService.recordSuppressed(
                    "PASSWORD_RESET_OTP_SUPPRESSED",
                    resetChannel,
                    normalizedIp,
                    "ACCOUNT_NOT_FOUND_OR_INELIGIBLE",
                    Map.of("identifierType", identifierType(normalizedIdentifier)));
            return PasswordResetResponse.accepted(GENERIC_OTP_MESSAGE);
        }

        User user = userCandidate.get();

        Instant now = Instant.now();
        Optional<PasswordResetRequestEntity> latestActive = resetRequestRepository
                .findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                        user.getId(),
                        resetChannel,
                        ACTIVE_STATUSES);
        latestActive.ifPresent(requestEntity -> expireIfNeeded(requestEntity, now));
        latestActive
                .filter(requestEntity -> requestEntity.getRequestStatus() == PasswordResetStatus.OTP_SENT)
                .filter(requestEntity -> isOtpStillValid(requestEntity, now))
                .ifPresent(requestEntity -> enforceResendCooldown(requestEntity, now));

        DeliveryDestination destination = resolveDeliveryDestination(user);
        if (destination == null) {
            auditService.recordSuppressed(
                    "PASSWORD_RESET_OTP_SUPPRESSED",
                    resetChannel,
                    normalizedIp,
                    "NO_REGISTERED_DELIVERY_CHANNEL",
                    Map.of("userId", user.getId()));
            return PasswordResetResponse.accepted(GENERIC_OTP_MESSAGE);
        }

        resetRequestRepository.cancelActiveRequestsForUser(
                user.getId(),
                ACTIVE_STATUSES,
                PasswordResetStatus.CANCELLED,
                now,
                null);

        String otp = otpGenerator.generateSixDigitOtp();
        PasswordResetRequestEntity resetRequest = new PasswordResetRequestEntity();
        resetRequest.setUser(user);
        resetRequest.setChannel(resetChannel);
        resetRequest.setOtpHash(passwordEncoder.encode(otp));
        resetRequest.setOtpExpiryTime(now.plusSeconds(properties.getOtpValiditySeconds()));
        resetRequest.setOtpVerified(false);
        resetRequest.setFailedAttempts(0);
        resetRequest.setMaxAttempts(Math.max(1, properties.getMaxAttempts()));
        resetRequest.setRequestStatus(PasswordResetStatus.OTP_SENT);
        resetRequest.setRequestedIp(normalizedIp);
        resetRequest.setUserAgent(truncate(userAgent, 1000));

        PasswordResetRequestEntity savedRequest = resetRequestRepository.save(resetRequest);
        dispatchOtp(destination, otp);

        auditService.record(
                "PASSWORD_RESET_OTP_SENT",
                savedRequest,
                resetChannel,
                normalizedIp,
                "OTP_SENT",
                Map.of("deliveryChannel", destination.channel().name()));

        return PasswordResetResponse.accepted(GENERIC_OTP_MESSAGE);
    }

    @Override
    public PasswordResetResponse verifyOtp(
            PasswordResetOtpVerifyRequest request,
            ResetPasswordChannel channel,
            String clientIp) {
        String normalizedIdentifier = normalizeIdentifier(request == null ? null : request.getIdentifier());
        String otp = request == null ? null : request.getOtp();
        ResetPasswordChannel resetChannel = requireChannel(channel);
        String normalizedIp = truncate(clientIp, 100);
        rateLimiter.checkOtpVerifyAllowed(normalizedIdentifier, normalizedIp);

        User user = resolveEligibleUser(normalizedIdentifier).orElseThrow(() -> new InvalidOtpException(properties.getMaxAttempts()));
        PasswordResetRequestEntity resetRequest = resetRequestRepository
                .findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                        user.getId(),
                        resetChannel,
                        ACTIVE_STATUSES)
                .orElseThrow(() -> new InvalidOtpException(properties.getMaxAttempts()));

        Instant now = Instant.now();
        if (resetRequest.getRequestStatus() != PasswordResetStatus.OTP_SENT) {
            throw new InvalidOtpException(remainingAttempts(resetRequest));
        }
        if (!isOtpStillValid(resetRequest, now)) {
            markExpired(resetRequest);
            auditService.record(
                    "PASSWORD_RESET_OTP_EXPIRED",
                    resetRequest,
                    resetChannel,
                    normalizedIp,
                    "OTP_EXPIRED",
                    null);
            throw new OtpExpiredException();
        }
        if (resetRequest.getFailedAttempts() >= resetRequest.getMaxAttempts()) {
            blockRequest(resetRequest);
            throw new OtpAttemptsExceededException();
        }
        if (!StringUtils.hasText(otp) || !passwordEncoder.matches(otp.trim(), resetRequest.getOtpHash())) {
            int failedAttempts = resetRequest.getFailedAttempts() + 1;
            resetRequest.setFailedAttempts(failedAttempts);
            if (failedAttempts >= resetRequest.getMaxAttempts()) {
                blockRequest(resetRequest);
                auditService.record(
                        "PASSWORD_RESET_OTP_BLOCKED",
                        resetRequest,
                        resetChannel,
                        normalizedIp,
                        "MAX_ATTEMPTS_EXCEEDED",
                        Map.of("failedAttempts", failedAttempts));
                throw new OtpAttemptsExceededException();
            }
            resetRequestRepository.save(resetRequest);
            auditService.record(
                    "PASSWORD_RESET_OTP_FAILED",
                    resetRequest,
                    resetChannel,
                    normalizedIp,
                    "INVALID_OTP",
                    Map.of("failedAttempts", failedAttempts));
            throw new InvalidOtpException(resetRequest.getMaxAttempts() - failedAttempts);
        }

        String resetToken = tokenGenerator.generateToken();
        resetRequest.setOtpVerified(true);
        resetRequest.setOtpVerifiedTime(now);
        resetRequest.setVerifiedIp(normalizedIp);
        resetRequest.setRequestStatus(PasswordResetStatus.OTP_VERIFIED);
        resetRequest.setResetTokenHash(tokenHasher.hashToken(resetToken));
        resetRequest.setResetTokenExpiryTime(now.plusSeconds(properties.getTokenValiditySeconds()));
        resetRequestRepository.save(resetRequest);

        auditService.record(
                "PASSWORD_RESET_OTP_VERIFIED",
                resetRequest,
                resetChannel,
                normalizedIp,
                "OTP_VERIFIED",
                null);

        return PasswordResetResponse.tokenIssued(
                OTP_VERIFIED_MESSAGE,
                resetToken,
                properties.getTokenValiditySeconds());
    }

    @Override
    public PasswordResetResponse resetPassword(
            ResetPasswordRequest request,
            ResetPasswordChannel channel,
            String clientIp) {
        ResetPasswordChannel resetChannel = requireChannel(channel);
        String normalizedIp = truncate(clientIp, 100);
        String tokenHash;
        try {
            tokenHash = tokenHasher.hashToken(request == null ? null : request.getResetToken());
        } catch (IllegalArgumentException ex) {
            throw new InvalidResetTokenException();
        }

        PasswordResetRequestEntity resetRequest = resetRequestRepository.findByResetTokenHash(tokenHash)
                .orElseThrow(InvalidResetTokenException::new);
        Instant now = Instant.now();

        if (resetRequest.getChannel() != resetChannel) {
            throw new InvalidResetTokenException();
        }
        if (resetRequest.getRequestStatus() != PasswordResetStatus.OTP_VERIFIED
                || !resetRequest.isOtpVerified()
                || !StringUtils.hasText(resetRequest.getResetTokenHash())) {
            throw new InvalidResetTokenException();
        }
        if (resetRequest.getResetTokenExpiryTime() == null
                || !now.isBefore(resetRequest.getResetTokenExpiryTime())) {
            markExpired(resetRequest);
            auditService.record(
                    "PASSWORD_RESET_TOKEN_EXPIRED",
                    resetRequest,
                    resetChannel,
                    normalizedIp,
                    "RESET_TOKEN_EXPIRED",
                    null);
            throw new ResetTokenExpiredException();
        }

        User user = resetRequest.getUser();
        String newPassword = validateNewPassword(request, user);
        user.setPassword(passwordEncoder.encode(newPassword));
        resetRequest.setRequestStatus(PasswordResetStatus.COMPLETED);
        resetRequest.setCompletedOn(now);
        resetRequest.setResetIp(normalizedIp);
        resetRequest.setResetTokenHash(null);
        resetRequest.setResetTokenExpiryTime(null);

        userRepository.save(user);
        resetRequestRepository.save(resetRequest);
        resetRequestRepository.cancelActiveRequestsForUser(
                user.getId(),
                ACTIVE_STATUSES,
                PasswordResetStatus.CANCELLED,
                now,
                resetRequest.getId());
        mobileRefreshTokenService.revokeActiveTokensForUser(user);

        auditService.record(
                "PASSWORD_RESET_COMPLETED",
                resetRequest,
                resetChannel,
                normalizedIp,
                "RESET_COMPLETED",
                null);
        sendResetCompletedNotification(user);
        return PasswordResetResponse.completed(RESET_COMPLETED_MESSAGE);
    }

    @Override
    public void invalidateExpiredRequests() {
        resetRequestRepository.expireRequests(ACTIVE_STATUSES, PasswordResetStatus.EXPIRED, Instant.now());
    }

    private Optional<User> resolveEligibleUser(String normalizedIdentifier) {
        Optional<User> user = resolveUser(normalizedIdentifier)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()));
        if (user.isEmpty()) {
            return Optional.empty();
        }

        try {
            agencyAccountAccessService.validateLoginAccess(user.get());
            return user;
        } catch (DisabledException ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Password reset eligibility check failed for userId={}", user.get().getId(), ex);
            return Optional.empty();
        }
    }

    private Optional<User> resolveUser(String normalizedIdentifier) {
        if (!StringUtils.hasText(normalizedIdentifier)) {
            return Optional.empty();
        }
        if (normalizedIdentifier.contains("@")) {
            return userRepository.findByEmailIgnoreCaseAndActiveTrue(normalizedIdentifier);
        }
        if (DIGITS.matcher(normalizedIdentifier).matches()) {
            return userRepository.findByMobileNoAndActiveTrue(normalizedIdentifier);
        }
        return employeeRepository.findByEmployeeCodeIgnoreCase(normalizedIdentifier)
                .map(EmployeeEntity::getUser)
                .filter(Objects::nonNull);
    }

    private DeliveryDestination resolveDeliveryDestination(User user) {
        String email = normalizeEmailOrNull(user.getEmail());
        String mobile = normalizeMobileOrNull(userAffiliationService.getAffiliation(user).getMobileNo());
        PasswordResetDeliveryChannel preference = properties.getDeliveryChannel() == null
                ? PasswordResetDeliveryChannel.EMAIL
                : properties.getDeliveryChannel();
        boolean emailAvailable = notificationChannelProperties.isEmailEnabled() && StringUtils.hasText(email);
        boolean smsAvailable = notificationChannelProperties.isSmsEnabled() && StringUtils.hasText(mobile);

        return switch (preference) {
            case BOTH -> {
                if (emailAvailable && smsAvailable) {
                    yield new DeliveryDestination(PasswordResetDeliveryChannel.BOTH, email, mobile);
                }
                yield emailAvailable
                        ? new DeliveryDestination(PasswordResetDeliveryChannel.EMAIL, email, null)
                        : smsAvailable ? new DeliveryDestination(PasswordResetDeliveryChannel.SMS, null, mobile) : null;
            }
            case SMS -> smsAvailable
                    ? new DeliveryDestination(PasswordResetDeliveryChannel.SMS, null, mobile)
                    : emailAvailable ? new DeliveryDestination(PasswordResetDeliveryChannel.EMAIL, email, null) : null;
            case EMAIL -> emailAvailable
                    ? new DeliveryDestination(PasswordResetDeliveryChannel.EMAIL, email, null)
                    : smsAvailable ? new DeliveryDestination(PasswordResetDeliveryChannel.SMS, null, mobile) : null;
        };
    }

    private void dispatchOtp(DeliveryDestination destination, String otp) {
        switch (destination.channel()) {
            case EMAIL -> otpDispatchService.sendEmailOtp(destination.email(), otp, VerificationPurposes.PASSWORD_RESET, null);
            case SMS -> otpDispatchService.sendMobileOtp(destination.mobile(), otp, null);
            case BOTH -> {
                otpDispatchService.sendEmailOtp(destination.email(), otp, VerificationPurposes.PASSWORD_RESET, null);
                otpDispatchService.sendMobileOtp(destination.mobile(), otp, null);
            }
        }
    }

    private String validateNewPassword(ResetPasswordRequest request, User user) {
        if (request == null) {
            throw new PasswordPolicyException("INVALID_PASSWORD_REQUEST", "Password reset request is required.");
        }
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new PasswordPolicyException(
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "New password and confirm password must match.");
        }

        String validatedPassword;
        try {
            validatedPassword = UserValidationUtil.validatePassword(request.getNewPassword());
        } catch (IllegalArgumentException ex) {
            throw new PasswordPolicyException("PASSWORD_POLICY_FAILED", ex.getMessage());
        }

        if (passwordEncoder.matches(validatedPassword, user.getPassword())) {
            throw new PasswordReuseException();
        }
        if (containsSensitiveIdentifier(validatedPassword, user)) {
            throw new PasswordPolicyException(
                    "PASSWORD_POLICY_FAILED",
                    "Password must not contain your username or employee code.");
        }
        return validatedPassword;
    }

    private boolean containsSensitiveIdentifier(String password, User user) {
        String normalizedPassword = normalizeForContainment(password);
        if (!StringUtils.hasText(normalizedPassword)) {
            return false;
        }

        if (containsCandidate(normalizedPassword, user.getEmail())
                || containsCandidate(normalizedPassword, emailLocalPart(user.getEmail()))
                || containsCandidate(normalizedPassword, user.getName())) {
            return true;
        }

        return employeeRepository.findByUser_Id(user.getId())
                .map(EmployeeEntity::getEmployeeCode)
                .filter(code -> containsCandidate(normalizedPassword, code))
                .isPresent();
    }

    private boolean containsCandidate(String normalizedPassword, String candidate) {
        String normalizedCandidate = normalizeForContainment(candidate);
        return normalizedCandidate != null
                && normalizedCandidate.length() >= 3
                && normalizedPassword.contains(normalizedCandidate);
    }

    private String normalizeForContainment(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String emailLocalPart(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private void sendResetCompletedNotification(User user) {
        try {
            accountNotificationService.sendPasswordResetCompleted(user.getEmail(), user.getMobileNo(), user.getName());
        } catch (RuntimeException ex) {
            log.warn("Password reset notification failed for userId={}", user.getId(), ex);
        }
    }

    private void enforceResendCooldown(PasswordResetRequestEntity requestEntity, Instant now) {
        Instant baseline = requestEntity.getUpdatedOn() == null ? requestEntity.getCreatedOn() : requestEntity.getUpdatedOn();
        if (baseline == null) {
            return;
        }
        Instant allowedAt = baseline.plusSeconds(Math.max(0, properties.getResendCooldownSeconds()));
        if (now.isBefore(allowedAt)) {
            throw new RateLimitExceededException(Math.max(1, Duration.between(now, allowedAt).getSeconds()));
        }
    }

    private void expireIfNeeded(PasswordResetRequestEntity requestEntity, Instant now) {
        if (requestEntity.getRequestStatus() == PasswordResetStatus.OTP_SENT && !isOtpStillValid(requestEntity, now)) {
            markExpired(requestEntity);
        } else if (requestEntity.getRequestStatus() == PasswordResetStatus.OTP_VERIFIED
                && (requestEntity.getResetTokenExpiryTime() == null || !now.isBefore(requestEntity.getResetTokenExpiryTime()))) {
            markExpired(requestEntity);
        }
    }

    private boolean isOtpStillValid(PasswordResetRequestEntity requestEntity, Instant now) {
        return requestEntity.getOtpExpiryTime() != null && now.isBefore(requestEntity.getOtpExpiryTime());
    }

    private void markExpired(PasswordResetRequestEntity requestEntity) {
        requestEntity.setRequestStatus(PasswordResetStatus.EXPIRED);
        resetRequestRepository.save(requestEntity);
    }

    private void blockRequest(PasswordResetRequestEntity requestEntity) {
        requestEntity.setRequestStatus(PasswordResetStatus.BLOCKED);
        resetRequestRepository.save(requestEntity);
    }

    private int remainingAttempts(PasswordResetRequestEntity requestEntity) {
        return Math.max(0, requestEntity.getMaxAttempts() - requestEntity.getFailedAttempts());
    }

    private ResetPasswordChannel requireChannel(ResetPasswordChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Password reset channel is required.");
        }
        return channel;
    }

    private String normalizeIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw new PasswordPolicyException("INVALID_IDENTIFIER", "Identifier is required.");
        }
        String trimmed = identifier.trim();
        String mobileCandidate = trimmed.replaceAll("\\s+", "");
        if (mobileCandidate.matches("^\\d{10,15}$")) {
            return mobileCandidate;
        }
        if (trimmed.contains("@")) {
            try {
                return UserValidationUtil.normalizeEmail(trimmed);
            } catch (IllegalArgumentException ex) {
                throw new PasswordPolicyException("INVALID_IDENTIFIER", "Please provide a valid identifier.");
            }
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String identifierType(String normalizedIdentifier) {
        if (!StringUtils.hasText(normalizedIdentifier)) {
            return "UNKNOWN";
        }
        if (normalizedIdentifier.contains("@")) {
            return "EMAIL";
        }
        if (DIGITS.matcher(normalizedIdentifier).matches()) {
            return "MOBILE";
        }
        return "EMPLOYEE_CODE";
    }

    private String normalizeEmailOrNull(String email) {
        try {
            return StringUtils.hasText(email) ? UserValidationUtil.normalizeEmail(email) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeMobileOrNull(String mobileNo) {
        try {
            return UserValidationUtil.normalizeOptionalMobile(mobileNo);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private record DeliveryDestination(PasswordResetDeliveryChannel channel, String email, String mobile) {
    }
}
