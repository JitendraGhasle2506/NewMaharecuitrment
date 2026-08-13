package com.maharecruitment.gov.in.web.service.mobile.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeDetails;
import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileContactUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileAuthenticatedUser;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeDetailsService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenIssue;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@ExtendWith(MockitoExtension.class)
class MobileProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AgencyCandidatePreOnboardingRepository preOnboardingRepository;

    @Mock
    private UserAffiliationService userAffiliationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MobileEmployeeDetailsService employeeDetailsService;

    @Mock
    private MobileTokenService tokenService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateContactSynchronizesUserEmployeePreOnboardingAndReturnsRefreshedToken() {
        authenticate("old@example.com");
        User user = user("old@example.com", "9876543210", "encoded-old");
        AgencyCandidatePreOnboardingEntity preOnboarding = preOnboarding();
        EmployeeEntity employee = employee(101L, "EMP101", "old@example.com", "9876543210", preOnboarding);

        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("old@example.com")).thenReturn(Optional.of(user));
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 10L)).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCaseAndEmployeeIdNot("new@example.com", 101L)).thenReturn(false);
        when(userRepository.existsByMobileNoAndIdNot("9123456789", 10L)).thenReturn(false);
        when(employeeRepository.existsByMobileAndEmployeeIdNot("9123456789", 101L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeDetailsService.loadForUser(user)).thenReturn(details("photo-data-uri"));
        when(tokenService.issueToken(any(MobileAuthenticatedUser.class))).thenReturn(token());

        MobileProfileResponse response = service().updateContact(
                new MobileProfileContactUpdateRequest(101L, " New@Example.COM ", "9123456789"));

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getMobileNo()).isEqualTo("9123456789");
        assertThat(employee.getEmail()).isEqualTo("new@example.com");
        assertThat(employee.getMobile()).isEqualTo("9123456789");
        assertThat(preOnboarding.getCandidateEmail()).isEqualTo("new@example.com");
        assertThat(preOnboarding.getCandidateMobile()).isEqualTo("9123456789");
        assertThat(response.success()).isTrue();
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.mobileNo()).isEqualTo("9123456789");
        assertThat(response.accessToken()).isEqualTo("new-token");
        verify(userAffiliationService).synchronizeUserProfile(user);
        verify(preOnboardingRepository).save(preOnboarding);
    }

    @Test
    void updateContactRejectsDuplicateEmail() {
        authenticate("old@example.com");
        User user = user("old@example.com", "9876543210", "encoded-old");
        EmployeeEntity employee = employee(101L, "EMP101", "old@example.com", "9876543210", preOnboarding());
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("old@example.com")).thenReturn(Optional.of(user));
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service().updateContact(
                new MobileProfileContactUpdateRequest(101L, "new@example.com", "9123456789")))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(409);
                    assertThat(ex.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });

        verify(userRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updatePhotoStoresMobileOnlyPhotoWithoutChangingSharedPhotoFields() {
        authenticate("employee@example.com");
        User user = user("employee@example.com", "9876543210", "encoded-old");
        AgencyCandidatePreOnboardingEntity preOnboarding = preOnboarding();
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com", "9876543210", preOnboarding);
        employee.setMobilePhotoPath("/uploads/old-mobile-profile.jpg");
        MockMultipartFile photo = new MockMultipartFile("photo", "profile.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("employee@example.com")).thenReturn(Optional.of(user));
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(fileStorageService.store(photo, "mobile-profile-photo"))
                .thenReturn(new FileUploadResult("profile.jpg", "stored.jpg", "/uploads/profile.jpg", "image/jpeg", 3));
        when(employeeDetailsService.loadForUser(user)).thenReturn(details("updated-photo"));

        MobileProfileResponse response = service().updatePhoto(101L, photo, " [0.123, 0.456] ");

        assertThat(preOnboarding.getPhotoOriginalName()).isNull();
        assertThat(preOnboarding.getPhotoFilePath()).isNull();
        assertThat(preOnboarding.getPhotoFileType()).isNull();
        assertThat(preOnboarding.getPhotoFileSize()).isNull();
        assertThat(preOnboarding.getEmbedding()).isEqualTo("[0.123, 0.456]");
        assertThat(employee.getPhotoPath()).isNull();
        assertThat(employee.getMobilePhotoPath()).isEqualTo("/uploads/profile.jpg");
        assertThat(employee.getEmbedding()).isEqualTo("[0.123, 0.456]");
        assertThat(response.photoUrl()).isEqualTo("updated-photo");
        assertThat(response.faceData()).isEqualTo("[0.123, 0.456]");
        assertThat(response.embedding()).isEqualTo("[0.123, 0.456]");
        verify(employeeRepository).save(employee);
        verify(preOnboardingRepository).save(preOnboarding);
        verify(fileStorageService).deleteQuietly("/uploads/old-mobile-profile.jpg");
    }

    @Test
    void updatePhotoSupportsEmployeeWithoutPreOnboardingProfile() {
        authenticate("employee@example.com");
        User user = user("employee@example.com", "9876543210", "encoded-old");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com", "9876543210", null);
        MockMultipartFile photo = new MockMultipartFile("photo", "profile.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("employee@example.com")).thenReturn(Optional.of(user));
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(fileStorageService.store(photo, "mobile-profile-photo"))
                .thenReturn(new FileUploadResult("profile.jpg", "stored.jpg", "/uploads/profile.jpg", "image/jpeg", 3));
        when(employeeDetailsService.loadForUser(user)).thenReturn(details("updated-photo"));

        MobileProfileResponse response = service().updatePhoto(101L, photo, null);

        assertThat(employee.getPhotoPath()).isNull();
        assertThat(employee.getMobilePhotoPath()).isEqualTo("/uploads/profile.jpg");
        assertThat(response.photoUrl()).isEqualTo("updated-photo");
        verify(employeeRepository).save(employee);
        verify(preOnboardingRepository, never()).save(any());
    }

    @Test
    void changePasswordUpdatesEncodedPassword() {
        authenticate("employee@example.com");
        User user = user("employee@example.com", "9876543210", "encoded-old");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com", "9876543210", preOnboarding());
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("employee@example.com")).thenReturn(Optional.of(user));
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("OldPass@123", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@123", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded-new");

        var response = service().changePassword(
                new MobilePasswordUpdateRequest(101L, "OldPass@123", "NewPass@123", "NewPass@123"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(user.getPasswordChangeRequired()).isFalse();
        assertThat(response.success()).isTrue();
        assertThat(response.employeeId()).isEqualTo(101L);
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        authenticate("employee@example.com");
        User user = user("employee@example.com", "9876543210", "encoded-old");
        EmployeeEntity employee = employee(101L, "EMP101", "employee@example.com", "9876543210", preOnboarding());
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("employee@example.com")).thenReturn(Optional.of(user));
        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("WrongPass@123", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> service().changePassword(
                new MobilePasswordUpdateRequest(101L, "WrongPass@123", "NewPass@123", "NewPass@123")))
                .isInstanceOfSatisfying(MobileApiException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                    assertThat(ex.getCode()).isEqualTo("CURRENT_PASSWORD_INVALID");
                });

        verify(userRepository, never()).save(any());
    }

    private MobileProfileServiceImpl service() {
        return new MobileProfileServiceImpl(
                new MobileEmployeeAccessService(userRepository, employeeRepository),
                userRepository,
                employeeRepository,
                preOnboardingRepository,
                userAffiliationService,
                passwordEncoder,
                fileStorageService,
                employeeDetailsService,
                tokenService);
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private User user(String email, String mobileNo, String password) {
        User user = new User();
        user.setId(10L);
        user.setName("Test Employee");
        user.setEmail(email);
        user.setMobileNo(mobileNo);
        user.setPassword(password);
        user.setActive(true);
        return user;
    }

    private EmployeeEntity employee(
            Long employeeId,
            String employeeCode,
            String email,
            String mobile,
            AgencyCandidatePreOnboardingEntity preOnboarding) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setEmail(email);
        employee.setMobile(mobile);
        employee.setFullName("Test Employee");
        employee.setStatus("ACTIVE");
        employee.setPreOnboarding(preOnboarding);
        return employee;
    }

    private AgencyCandidatePreOnboardingEntity preOnboarding() {
        AgencyCandidatePreOnboardingEntity preOnboarding = new AgencyCandidatePreOnboardingEntity();
        preOnboarding.setPreOnboardingId(501L);
        preOnboarding.setCandidateEmail("old@example.com");
        preOnboarding.setCandidateMobile("9876543210");
        return preOnboarding;
    }

    private MobileEmployeeDetails details(String photoUrl) {
        return new MobileEmployeeDetails(
                101L,
                "EMP101",
                "Test Employee",
                photoUrl,
                "[0.123, 0.456]",
                null,
                null,
                null,
                null,
                null,
                null,
                "INTERNAL",
                null,
                null,
                null,
                null);
    }

    private MobileTokenIssue token() {
        return new MobileTokenIssue(
                "new-token",
                "Bearer",
                Instant.parse("2026-07-06T10:00:00Z"),
                Instant.parse("2026-07-06T11:00:00Z"),
                3600);
    }
}
