package com.maharecruitment.gov.in.auth.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.UserPasswordChangeRequest;
import com.maharecruitment.gov.in.auth.dto.UserProfileUpdateRequest;
import com.maharecruitment.gov.in.auth.dto.UserProfileView;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.CurrentUserProfileService;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;

@Service
@Transactional
public class CurrentUserProfileServiceImpl implements CurrentUserProfileService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CurrentUserProfileServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileView getProfile(String email) {
        return toView(loadUser(email));
    }

    @Override
    public UserProfileView updateProfile(String email, UserProfileUpdateRequest request) {
        User user = loadUser(email);
        user.setName(UserValidationUtil.normalizeName(request.getName()));
        user.setMobileNo(UserValidationUtil.normalizeOptionalMobile(request.getMobileNo()));

        User saved = userRepository.save(user);
        log.info("Current user profile updated. id={}, email={}", saved.getId(), saved.getEmail());
        return toView(saved);
    }

    @Override
    public void changePassword(String email, UserPasswordChangeRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }

        User user = loadUser(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        String validatedPassword = UserValidationUtil.validatePassword(request.getNewPassword());
        if (passwordEncoder.matches(validatedPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        user.setPassword(passwordEncoder.encode(validatedPassword));
        userRepository.save(user);
        log.info("Current user password changed. id={}, email={}", user.getId(), user.getEmail());
    }

    private User loadUser(String email) {
        String normalizedEmail = UserValidationUtil.normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + normalizedEmail));
    }

    private UserProfileView toView(User user) {
        List<String> roleNames = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(role -> role.getName())
                        .filter(Objects::nonNull)
                        .sorted(Comparator.naturalOrder())
                        .toList();

        Long departmentRegistrationId = user.getDepartmentRegistrationId() == null
                ? null
                : user.getDepartmentRegistrationId().getDepartmentRegistrationId();
        String departmentName = user.getDepartmentRegistrationId() == null
                ? null
                : user.getDepartmentRegistrationId().getDepartmentName();

        return UserProfileView.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .mobileNo(user.getMobileNo())
                .departmentRegistrationId(departmentRegistrationId)
                .departmentName(departmentName)
                .roleNames(roleNames)
                .build();
    }
}
