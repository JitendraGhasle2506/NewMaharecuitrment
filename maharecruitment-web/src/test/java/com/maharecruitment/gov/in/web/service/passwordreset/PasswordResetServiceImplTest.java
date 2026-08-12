package com.maharecruitment.gov.in.web.service.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.UserAffiliationView;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.AgencyAccountAccessService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
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

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordResetTokenHasher tokenHasher = new PasswordResetTokenHasher();
    private final List<PasswordResetRequestEntity> savedRequests = new ArrayList<>();

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserAffiliationService userAffiliationService;
    @Mock
    private AgencyAccountAccessService agencyAccountAccessService;
    @Mock
    private PasswordResetRequestRepository resetRequestRepository;
    @Mock
    private SecureOtpGenerator otpGenerator;
    @Mock
    private SecureResetTokenGenerator tokenGenerator;
    @Mock
    private PasswordResetRateLimiter rateLimiter;
    @Mock
    private OtpDispatchService otpDispatchService;
    @Mock
    private AccountNotificationService accountNotificationService;
    @Mock
    private MobileRefreshTokenService mobileRefreshTokenService;
    @Mock
    private PasswordResetAuditService auditService;

    private PasswordResetProperties properties;
    private NotificationChannelProperties notificationProperties;
    private PasswordResetServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setOtpValidityMinutes(5);
        properties.setTokenValidityMinutes(10);
        properties.setMaxAttempts(5);

        notificationProperties = new NotificationChannelProperties();
        notificationProperties.setEmailEnabled(true);
        notificationProperties.setSmsEnabled(true);

        lenient().when(resetRequestRepository.save(any(PasswordResetRequestEntity.class))).thenAnswer(invocation -> {
            PasswordResetRequestEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId((long) savedRequests.size() + 1L);
            }
            savedRequests.add(entity);
            return entity;
        });

        service = new PasswordResetServiceImpl(
                userRepository,
                employeeRepository,
                userAffiliationService,
                agencyAccountAccessService,
                resetRequestRepository,
                passwordEncoder,
                otpGenerator,
                tokenGenerator,
                tokenHasher,
                rateLimiter,
                properties,
                notificationProperties,
                otpDispatchService,
                accountNotificationService,
                mobileRefreshTokenService,
                auditService);
    }

    @Test
    void unknownUserReceivesGenericResponseWithoutOtpDispatch() {
        PasswordResetOtpRequest request = otpRequest("missing@example.com");
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("missing@example.com")).thenReturn(Optional.empty());

        PasswordResetResponse response = service.requestOtp(request, ResetPasswordChannel.MOBILE_API, "127.0.0.1", "JUnit");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResetToken()).isNull();
        assertThat(response.getMessage()).contains("If the account information is valid");
        verify(resetRequestRepository, never()).save(any());
        verify(otpDispatchService, never()).sendEmailOtp(any(), any(), any(), any());
    }

    @Test
    void requestOtpStoresOnlyOtpHashAndDispatchesPasswordResetEmail() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
        when(userAffiliationService.getAffiliation(user)).thenReturn(affiliation(user));
        when(resetRequestRepository.findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                eq(user.getId()), eq(ResetPasswordChannel.WEB), anyCollection()))
                        .thenReturn(Optional.empty());
        when(otpGenerator.generateSixDigitOtp()).thenReturn("238901");

        service.requestOtp(otpRequest(user.getEmail()), ResetPasswordChannel.WEB, "127.0.0.1", "JUnit");

        PasswordResetRequestEntity saved = savedRequests.getFirst();
        assertThat(saved.getOtpHash()).isNotEqualTo("238901");
        assertThat(passwordEncoder.matches("238901", saved.getOtpHash())).isTrue();
        assertThat(saved.getRequestStatus()).isEqualTo(PasswordResetStatus.OTP_SENT);
        verify(otpDispatchService).sendEmailOtp(
                user.getEmail(),
                "238901",
                VerificationPurposes.PASSWORD_RESET,
                null);
    }

    @Test
    void correctOtpIssuesSingleUseResetToken() {
        User user = activeUser();
        PasswordResetRequestEntity resetRequest = otpSentRequest(user, "123456");
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
        when(resetRequestRepository.findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                eq(user.getId()), eq(ResetPasswordChannel.MOBILE_API), anyCollection()))
                        .thenReturn(Optional.of(resetRequest));
        when(tokenGenerator.generateToken()).thenReturn("raw-reset-token");

        PasswordResetOtpVerifyRequest verifyRequest = new PasswordResetOtpVerifyRequest();
        verifyRequest.setIdentifier(user.getEmail());
        verifyRequest.setOtp("123456");

        PasswordResetResponse response = service.verifyOtp(verifyRequest, ResetPasswordChannel.MOBILE_API, "127.0.0.1");

        assertThat(response.getResetToken()).isEqualTo("raw-reset-token");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600L);
        assertThat(resetRequest.getRequestStatus()).isEqualTo(PasswordResetStatus.OTP_VERIFIED);
        assertThat(resetRequest.isOtpVerified()).isTrue();
        assertThat(resetRequest.getOtpHash()).isNull();
        assertThat(resetRequest.getResetTokenHash()).hasSize(64).doesNotContain("raw-reset-token");
    }

    @Test
    void incorrectOtpIncrementsFailedAttempt() {
        User user = activeUser();
        PasswordResetRequestEntity resetRequest = otpSentRequest(user, "123456");
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
        when(resetRequestRepository.findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                eq(user.getId()), eq(ResetPasswordChannel.MOBILE_API), anyCollection()))
                        .thenReturn(Optional.of(resetRequest));

        PasswordResetOtpVerifyRequest verifyRequest = new PasswordResetOtpVerifyRequest();
        verifyRequest.setIdentifier(user.getEmail());
        verifyRequest.setOtp("000000");

        assertThatThrownBy(() -> service.verifyOtp(verifyRequest, ResetPasswordChannel.MOBILE_API, "127.0.0.1"))
                .isInstanceOf(InvalidOtpException.class);

        assertThat(resetRequest.getFailedAttempts()).isEqualTo(1);
        assertThat(resetRequest.getRequestStatus()).isEqualTo(PasswordResetStatus.OTP_SENT);
    }

    @Test
    void fifthIncorrectOtpPersistsTemporaryBlockAndInvalidatesOtp() {
        User user = activeUser();
        PasswordResetRequestEntity resetRequest = otpSentRequest(user, "123456");
        resetRequest.setFailedAttempts(4);
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
        when(resetRequestRepository.findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                eq(user.getId()), eq(ResetPasswordChannel.MOBILE_API), anyCollection()))
                        .thenReturn(Optional.of(resetRequest));
        PasswordResetOtpVerifyRequest verifyRequest = new PasswordResetOtpVerifyRequest();
        verifyRequest.setIdentifier(user.getEmail());
        verifyRequest.setOtp("000000");

        assertThatThrownBy(() -> service.verifyOtp(verifyRequest, ResetPasswordChannel.MOBILE_API, "127.0.0.1"))
                .isInstanceOf(OtpAttemptsExceededException.class)
                .satisfies(exception -> assertThat(((PasswordResetException) exception).getRetryAfterSeconds())
                        .isEqualTo(900L));

        assertThat(resetRequest.getFailedAttempts()).isEqualTo(5);
        assertThat(resetRequest.getRequestStatus()).isEqualTo(PasswordResetStatus.BLOCKED);
        assertThat(resetRequest.getOtpHash()).isNull();
        assertThat(resetRequest.getOtpLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void activePasswordResetBlockCannotBeBypassedByRequestingAnotherOtp() {
        User user = activeUser();
        PasswordResetRequestEntity blockedRequest = otpSentRequest(user, "123456");
        blockedRequest.setRequestStatus(PasswordResetStatus.BLOCKED);
        blockedRequest.setOtpHash(null);
        blockedRequest.setOtpLockedUntil(Instant.now().plusSeconds(900));
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
        when(resetRequestRepository.findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
                eq(user.getId()), eq(ResetPasswordChannel.WEB), anyCollection()))
                        .thenReturn(Optional.of(blockedRequest));

        assertThatThrownBy(() -> service.requestOtp(
                otpRequest(user.getEmail()),
                ResetPasswordChannel.WEB,
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(OtpTemporarilyBlockedException.class);

        verify(otpGenerator, never()).generateSixDigitOtp();
        verify(otpDispatchService, never()).sendEmailOtp(any(), any(), any(), any());
    }

    @Test
    void passwordResetOtpFailuresDoNotRollBackAttemptAndBlockState() {
        Transactional transaction = Arrays.stream(PasswordResetServiceImpl.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("verifyOtp"))
                .findFirst()
                .orElseThrow()
                .getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(Arrays.asList(transaction.noRollbackFor())).contains(PasswordResetException.class);
    }

    @Test
    void resetPasswordUpdatesEncodedPasswordCompletesRequestAndRevokesMobileRefreshTokens() {
        User user = activeUser();
        String resetToken = "valid-reset-token";
        PasswordResetRequestEntity resetRequest = otpVerifiedRequest(user, resetToken);
        when(resetRequestRepository.findByResetTokenHash(tokenHasher.hashToken(resetToken))).thenReturn(Optional.of(resetRequest));
        when(employeeRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken(resetToken);
        request.setNewPassword("Fresh@Pass123");
        request.setConfirmPassword("Fresh@Pass123");

        PasswordResetResponse response = service.resetPassword(request, ResetPasswordChannel.WEB, "127.0.0.1");

        assertThat(response.isSuccess()).isTrue();
        assertThat(passwordEncoder.matches("Fresh@Pass123", user.getPassword())).isTrue();
        assertThat(resetRequest.getRequestStatus()).isEqualTo(PasswordResetStatus.COMPLETED);
        assertThat(resetRequest.getResetTokenHash()).isNull();
        assertThat(resetRequest.getCompletedOn()).isNotNull();
        verify(userRepository).save(user);
        verify(mobileRefreshTokenService).revokeActiveTokensForUser(user);
        verify(accountNotificationService).sendPasswordResetCompleted(user.getEmail(), user.getMobileNo(), user.getName());
    }

    @Test
    void resetTokenCannotBeReusedAfterCompletion() {
        String resetToken = "used-reset-token";
        when(resetRequestRepository.findByResetTokenHash(tokenHasher.hashToken(resetToken))).thenReturn(Optional.empty());
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken(resetToken);
        request.setNewPassword("Fresh@Pass123");
        request.setConfirmPassword("Fresh@Pass123");

        assertThatThrownBy(() -> service.resetPassword(request, ResetPasswordChannel.WEB, "127.0.0.1"))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    private PasswordResetOtpRequest otpRequest(String identifier) {
        PasswordResetOtpRequest request = new PasswordResetOtpRequest();
        request.setIdentifier(identifier);
        return request;
    }

    private PasswordResetRequestEntity otpSentRequest(User user, String otp) {
        PasswordResetRequestEntity request = new PasswordResetRequestEntity();
        request.setId(20L);
        request.setUser(user);
        request.setChannel(ResetPasswordChannel.MOBILE_API);
        request.setOtpHash(passwordEncoder.encode(otp));
        request.setOtpExpiryTime(Instant.now().plusSeconds(300));
        request.setMaxAttempts(5);
        request.setRequestStatus(PasswordResetStatus.OTP_SENT);
        return request;
    }

    private PasswordResetRequestEntity otpVerifiedRequest(User user, String resetToken) {
        PasswordResetRequestEntity request = new PasswordResetRequestEntity();
        request.setId(21L);
        request.setUser(user);
        request.setChannel(ResetPasswordChannel.WEB);
        request.setOtpHash(passwordEncoder.encode("123456"));
        request.setOtpExpiryTime(Instant.now().plusSeconds(300));
        request.setOtpVerified(true);
        request.setOtpVerifiedTime(Instant.now());
        request.setMaxAttempts(5);
        request.setRequestStatus(PasswordResetStatus.OTP_VERIFIED);
        request.setResetTokenHash(tokenHasher.hashToken(resetToken));
        request.setResetTokenExpiryTime(Instant.now().plusSeconds(600));
        return request;
    }

    private User activeUser() {
        User user = new User();
        user.setId(10L);
        user.setName("Reset User");
        user.setEmail("reset@example.com");
        user.setMobileNo("9876543210");
        user.setPassword(passwordEncoder.encode("Old@Pass123"));
        user.setActive(true);
        return user;
    }

    private UserAffiliationView affiliation(User user) {
        return UserAffiliationView.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .mobileNo(user.getMobileNo())
                .roleNames(List.of("ROLE_EMPLOYEE"))
                .build();
    }
}
