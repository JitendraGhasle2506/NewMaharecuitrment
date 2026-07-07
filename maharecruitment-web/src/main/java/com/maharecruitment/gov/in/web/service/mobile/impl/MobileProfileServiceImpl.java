package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.util.AuthorityUtil;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeDetails;
import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileContactUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticatedUser;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessContext;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeDetailsService;
import com.maharecruitment.gov.in.web.service.mobile.MobileProfileService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
public class MobileProfileServiceImpl implements MobileProfileService {

    private static final String EMPLOYEE_PHOTO_MODULE = "employee-photo";
    private static final int MAX_EMBEDDING_LENGTH = 200_000;

    private final MobileEmployeeAccessService mobileEmployeeAccessService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    private final UserAffiliationService userAffiliationService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final MobileEmployeeDetailsService employeeDetailsService;
    private final MobileTokenService tokenService;

    public MobileProfileServiceImpl(
            MobileEmployeeAccessService mobileEmployeeAccessService,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            UserAffiliationService userAffiliationService,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService,
            MobileEmployeeDetailsService employeeDetailsService,
            MobileTokenService tokenService) {
        this.mobileEmployeeAccessService = mobileEmployeeAccessService;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.preOnboardingRepository = preOnboardingRepository;
        this.userAffiliationService = userAffiliationService;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.employeeDetailsService = employeeDetailsService;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public MobileProfileResponse getProfile(Long employeeId) {
        MobileEmployeeAccessContext context = mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(employeeId);
        return toProfileResponse(context.user(), "Mobile profile fetched successfully.", null);
    }

    @Override
    @Transactional
    public MobileProfileResponse updateContact(MobileProfileContactUpdateRequest request) {
        if (request == null) {
            throw badRequest("INVALID_PROFILE_REQUEST", "Profile update request is required.");
        }

        MobileEmployeeAccessContext context = mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(request.employeeId());
        User user = context.user();
        EmployeeEntity employee = context.employee();

        String normalizedEmail = StringUtils.hasText(request.email())
                ? normalizeEmail(request.email())
                : user.getEmail();
        String normalizedMobile = StringUtils.hasText(request.mobileNo())
                ? normalizeMobile(request.mobileNo())
                : user.getMobileNo();

        boolean emailChanged = !equalsIgnoreCase(user.getEmail(), normalizedEmail);
        boolean mobileChanged = !Objects.equals(normalizeNullableText(user.getMobileNo()), normalizeNullableText(normalizedMobile));
        if (!emailChanged && !mobileChanged) {
            throw badRequest("PROFILE_NOT_CHANGED", "Provide a new email address or mobile number to update.");
        }

        validateUniqueEmail(user.getId(), employee.getEmployeeId(), normalizedEmail);
        validateUniqueMobile(user.getId(), employee.getEmployeeId(), normalizedMobile);

        applyContact(user, employee, normalizedEmail, normalizedMobile);

        User savedUser = userRepository.save(user);
        employeeRepository.save(employee);
        savePreOnboardingContact(employee.getPreOnboarding(), normalizedEmail, normalizedMobile);
        userAffiliationService.synchronizeUserProfile(savedUser);

        MobileTokenIssue refreshedToken = tokenService.issueToken(toAuthenticatedUser(savedUser));
        return toProfileResponse(savedUser, "Mobile profile updated successfully.", refreshedToken);
    }

    @Override
    @Transactional
    public MobileProfileResponse updatePhoto(Long employeeId, MultipartFile photo, String embedding) {
        MobileEmployeeAccessContext context = mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(employeeId);
        if (photo == null || photo.isEmpty()) {
            throw badRequest("PHOTO_REQUIRED", "Photo file is required.");
        }

        EmployeeEntity employee = context.employee();
        AgencyCandidatePreOnboardingEntity preOnboarding = employee.getPreOnboarding();
        if (preOnboarding == null || preOnboarding.getPreOnboardingId() == null) {
            throw badRequest("ONBOARDING_PROFILE_NOT_FOUND", "Employee onboarding profile is not available.");
        }

        FileUploadResult uploadResult = fileStorageService.store(photo, EMPLOYEE_PHOTO_MODULE);
        preOnboarding.setPhotoFilePath(uploadResult.fullPath());
        preOnboarding.setPhotoOriginalName(uploadResult.originalFileName());
        preOnboarding.setPhotoFileType(uploadResult.contentType());
        preOnboarding.setPhotoFileSize(uploadResult.size());
        if (embedding != null) {
            String normalizedEmbedding = normalizeEmbedding(embedding);
            employee.setEmbedding(normalizedEmbedding);
            preOnboarding.setEmbedding(normalizedEmbedding);
            employeeRepository.save(employee);
        }
        preOnboardingRepository.save(preOnboarding);

        return toProfileResponse(context.user(), "Profile photo updated successfully.", null);
    }

    @Override
    @Transactional
    public MobilePasswordUpdateResponse changePassword(MobilePasswordUpdateRequest request) {
        return updatePassword(request, "Password changed successfully.");
    }

    @Override
    @Transactional
    public MobilePasswordUpdateResponse resetPassword(MobilePasswordUpdateRequest request) {
        return updatePassword(request, "Password reset successfully.");
    }

    private MobilePasswordUpdateResponse updatePassword(MobilePasswordUpdateRequest request, String message) {
        if (request == null) {
            throw badRequest("INVALID_PASSWORD_REQUEST", "Password update request is required.");
        }

        MobileEmployeeAccessContext context = mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(request.employeeId());
        User user = context.user();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new MobileApiException(
                    HttpStatus.FORBIDDEN,
                    "CURRENT_PASSWORD_INVALID",
                    "Current password is incorrect.");
        }

        String newPassword = validateNewPassword(request.newPassword(), request.confirmPassword());
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw badRequest("PASSWORD_REUSE_NOT_ALLOWED", "New password must be different from current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return new MobilePasswordUpdateResponse(true, message, user.getId(), context.employee().getEmployeeId());
    }

    private void applyContact(User user, EmployeeEntity employee, String email, String mobileNo) {
        user.setEmail(email);
        user.setMobileNo(mobileNo);
        employee.setEmail(email);
        employee.setMobile(mobileNo);
    }

    private void savePreOnboardingContact(
            AgencyCandidatePreOnboardingEntity preOnboarding,
            String email,
            String mobileNo) {
        if (preOnboarding == null || preOnboarding.getPreOnboardingId() == null) {
            return;
        }
        preOnboarding.setCandidateEmail(email);
        preOnboarding.setCandidateMobile(mobileNo);
        preOnboardingRepository.save(preOnboarding);
    }

    private void validateUniqueEmail(Long userId, Long employeeId, String email) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)
                || employeeRepository.existsByEmailIgnoreCaseAndEmployeeIdNot(email, employeeId)) {
            throw new MobileApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "Email address is already registered.");
        }
    }

    private void validateUniqueMobile(Long userId, Long employeeId, String mobileNo) {
        if (!StringUtils.hasText(mobileNo)) {
            throw badRequest("MOBILE_REQUIRED", "Mobile number is required.");
        }
        if (userRepository.existsByMobileNoAndIdNot(mobileNo, userId)
                || employeeRepository.existsByMobileAndEmployeeIdNot(mobileNo, employeeId)) {
            throw new MobileApiException(
                    HttpStatus.CONFLICT,
                    "MOBILE_ALREADY_EXISTS",
                    "Mobile number is already registered.");
        }
    }

    private MobileProfileResponse toProfileResponse(User user, String message, MobileTokenIssue token) {
        MobileEmployeeDetails details = employeeDetailsService.loadForUser(user);
        return new MobileProfileResponse(
                true,
                message,
                user.getId(),
                details.empId(),
                details.employeeCode(),
                details.employeeName(),
                user.getEmail(),
                user.getMobileNo(),
                details.photoUrl(),
                details.faceData(),
                token != null ? token.tokenType() : null,
                token != null ? token.accessToken() : null,
                token != null ? token.expiresInSeconds() : null,
                token != null ? token.expiresAt() : null);
    }

    private MobileAuthenticatedUser toAuthenticatedUser(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(role -> AuthorityUtil.toAuthority(role.getName()))
                        .filter(StringUtils::hasText)
                        .distinct()
                        .sorted()
                        .toList();
        return new MobileAuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNo(),
                roles);
    }

    private String validateNewPassword(String newPassword, String confirmPassword) {
        if (!Objects.equals(newPassword, confirmPassword)) {
            throw badRequest("PASSWORD_CONFIRMATION_MISMATCH", "New password and confirm password must match.");
        }
        try {
            return UserValidationUtil.validatePassword(newPassword);
        } catch (IllegalArgumentException ex) {
            throw badRequest("INVALID_PASSWORD", ex.getMessage());
        }
    }

    private String normalizeEmail(String email) {
        try {
            return UserValidationUtil.normalizeEmail(email);
        } catch (IllegalArgumentException ex) {
            throw badRequest("INVALID_EMAIL", ex.getMessage());
        }
    }

    private String normalizeMobile(String mobileNo) {
        try {
            return UserValidationUtil.normalizeOptionalMobile(mobileNo);
        } catch (IllegalArgumentException ex) {
            throw badRequest("INVALID_MOBILE", ex.getMessage());
        }
    }

    private String normalizeEmbedding(String embedding) {
        String normalized = StringUtils.hasText(embedding) ? embedding.trim() : null;
        if (normalized != null && normalized.length() > MAX_EMBEDDING_LENGTH) {
            throw badRequest("EMBEDDING_TOO_LARGE", "Embedding must not exceed 200000 characters.");
        }
        return normalized;
    }

    private boolean equalsIgnoreCase(String first, String second) {
        String normalizedFirst = normalizeNullableText(first);
        String normalizedSecond = normalizeNullableText(second);
        return normalizedFirst == null
                ? normalizedSecond == null
                : normalizedFirst.equalsIgnoreCase(normalizedSecond);
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private MobileApiException badRequest(String code, String message) {
        return new MobileApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
