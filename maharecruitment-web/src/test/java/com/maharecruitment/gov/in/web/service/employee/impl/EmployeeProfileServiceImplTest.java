package com.maharecruitment.gov.in.web.service.employee.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProfile;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProfileRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@ExtendWith(MockitoExtension.class)
class EmployeeProfileServiceImplTest {

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Test
    void updateProfileSynchronizesChangedEmailToUserAndEmployeeMaster() {
        User user = user();
        EmployeeEntity employee = employee();
        EmployeeProfileDTO dto = new EmployeeProfileDTO();
        dto.setEmail(" New.Employee@Example.COM ");
        dto.setMobileNo("9876543210");

        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("old.employee@example.com"))
                .thenReturn(Optional.of(user));
        when(employeeRepository.findDetailedByUserId(10L)).thenReturn(Optional.of(employee));
        when(employeeProfileRepository.findByEmployeeEmployeeId(101L)).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new.employee@example.com", 10L)).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCaseAndEmployeeIdNot("new.employee@example.com", 101L))
                .thenReturn(false);
        when(employeeProfileRepository.save(any(EmployeeProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(userRepository.save(user)).thenReturn(user);

        service().updateCurrentEmployeeProfile("old.employee@example.com", dto);

        assertThat(user.getEmail()).isEqualTo("new.employee@example.com");
        assertThat(employee.getEmail()).isEqualTo("new.employee@example.com");
        verify(userRepository).save(user);
        verify(employeeRepository).save(employee);
    }

    @Test
    void resolvesEmployeeProfilePhotoBeforeOtherPhotoSources() {
        User user = user();
        EmployeeEntity employee = employee();
        employee.setPhotoPath("D:/uploads/employee-master/master.jpg");
        EmployeeProfile profile = new EmployeeProfile();
        profile.setPhotoPath("D:/uploads/employee-profile-photo/profile.jpg");
        Path resolvedPath = Path.of("D:/uploads/employee-profile-photo/profile.jpg");
        preparePhotoLookup(user, employee, Optional.of(profile));
        allowPhoto(profile.getPhotoPath(), resolvedPath);

        assertThat(service().resolveCurrentEmployeePhoto(user.getEmail())).contains(resolvedPath);
        verify(fileStorageService, never())
                .isManagedFileAllowed(employee.getPhotoPath(), "employee-profile-photo");
    }

    @Test
    void resolvesEmployeeMasterPhotoWhenProfilePhotoIsAbsent() {
        User user = user();
        EmployeeEntity employee = employee();
        employee.setPhotoPath("D:/uploads/employee-master/master.png");
        Path resolvedPath = Path.of("D:/uploads/employee-master/master.png");
        preparePhotoLookup(user, employee, Optional.empty());
        allowPhoto(employee.getPhotoPath(), resolvedPath);

        assertThat(service().resolveCurrentEmployeePhoto(user.getEmail())).contains(resolvedPath);
    }

    @Test
    void resolvesLegacyPreOnboardingPhotoAsLastFallback() {
        User user = user();
        EmployeeEntity employee = employee();
        AgencyCandidatePreOnboardingEntity preOnboarding = new AgencyCandidatePreOnboardingEntity();
        preOnboarding.setPhotoFilePath("D:/uploads/pre-onboarding/legacy.jpeg");
        employee.setPreOnboarding(preOnboarding);
        Path resolvedPath = Path.of("D:/uploads/pre-onboarding/legacy.jpeg");
        preparePhotoLookup(user, employee, Optional.empty());
        allowPhoto(preOnboarding.getPhotoFilePath(), resolvedPath);

        assertThat(service().resolveCurrentEmployeePhoto(user.getEmail())).contains(resolvedPath);
    }

    @Test
    void returnsNoPhotoWhenNoValidPhotoSourceExists() {
        User user = user();
        EmployeeEntity employee = employee();
        employee.setPhotoPath("D:/uploads/employee-master/missing.jpg");
        preparePhotoLookup(user, employee, Optional.empty());
        when(fileStorageService.isManagedFileAllowed(
                employee.getPhotoPath(),
                "employee-profile-photo")).thenReturn(false);

        assertThat(service().resolveCurrentEmployeePhoto(user.getEmail())).isEmpty();
    }

    private void preparePhotoLookup(
            User user,
            EmployeeEntity employee,
            Optional<EmployeeProfile> profile) {
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
        when(employeeRepository.findDetailedByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(employeeProfileRepository.findByEmployeeEmployeeId(employee.getEmployeeId())).thenReturn(profile);
    }

    private void allowPhoto(String photoPath, Path resolvedPath) {
        when(fileStorageService.isManagedFileAllowed(photoPath, "employee-profile-photo")).thenReturn(true);
        when(fileStorageService.resolveManagedPath(photoPath)).thenReturn(Optional.of(resolvedPath));
    }

    private EmployeeProfileServiceImpl service() {
        return new EmployeeProfileServiceImpl(
                employeeProfileRepository,
                employeeRepository,
                userRepository,
                fileStorageService);
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setName("Old Employee");
        user.setEmail("old.employee@example.com");
        user.setMobileNo("9876543210");
        user.setActive(true);
        return user;
    }

    private EmployeeEntity employee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(101L);
        employee.setEmployeeCode("EMP101");
        employee.setFullName("Old Employee");
        employee.setEmail("old.employee@example.com");
        employee.setMobile("9876543210");
        employee.setStatus("ACTIVE");
        return employee;
    }
}
