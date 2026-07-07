package com.maharecruitment.gov.in.web.service.employee.impl;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProfile;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProfileRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.exception.FileStorageException;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmployeeProfileServiceImpl implements EmployeeProfileService {

    private static final String EMPLOYEE_PHOTO_MODULE = "employee-profile-photo";
    private static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final LocalDate DEFAULT_DOB = LocalDate.of(1900, 1, 1);

    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public EmployeeProfileServiceImpl(
            EmployeeProfileRepository employeeProfileRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.employeeProfileRepository = employeeProfileRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileDTO getCurrentEmployeeProfile(String loginEmail) {
        User user = requireUser(loginEmail);
        EmployeeEntity employee = requireEmployee(loginEmail);
        EmployeeProfile profile = employeeProfileRepository.findByEmployeeEmployeeId(employee.getEmployeeId()).orElse(null);
        return toDto(profile, user, employee);
    }

    @Override
    @Transactional
    public EmployeeProfileDTO updateCurrentEmployeeProfile(String loginEmail, EmployeeProfileDTO profileDTO) {
        User user = requireUser(loginEmail);
        EmployeeEntity employee = requireEmployee(loginEmail);
        EmployeeProfile profile = employeeProfileRepository.findByEmployeeEmployeeId(employee.getEmployeeId()).orElseGet(() -> {
            EmployeeProfile created = new EmployeeProfile();
            created.setEmployee(employee);
            created.setCreatedBy(user.getEmail());
            return created;
        });

        profile.setDob(normalizeDob(profileDTO.getDob()));
        profile.setGender(normalizeText(profileDTO.getGender()));
        profile.setAlternateMobileNo(normalizeText(profileDTO.getAlternateMobileNo()));
        profile.setPanNo(normalizePan(profileDTO.getPanNo()));
        profile.setMaritalStatus(normalizeText(profileDTO.getMaritalStatus()));
        profile.setBloodGroup(normalizeText(profileDTO.getBloodGroup()));
        profile.setEmergencyContactName(normalizeText(profileDTO.getEmergencyContactName()));
        profile.setEmergencyContactNo(normalizeText(profileDTO.getEmergencyContactNo()));
        profile.setCurrentAddress(normalizeText(profileDTO.getCurrentAddress()));
        profile.setPermanentAddress(normalizeText(profileDTO.getPermanentAddress()));
        profile.setUpdatedBy(user.getEmail());
        syncEmployeeMaster(employee, profileDTO, profile);

        EmployeeProfile savedProfile = employeeProfileRepository.save(profile);
        EmployeeEntity savedEmployee = employeeRepository.save(employee);
        log.info("Employee profile saved for employeeId={} userId={}", employee.getEmployeeId(), user.getId());
        return toDto(savedProfile, user, savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeProfileDTO uploadCurrentEmployeePhoto(String loginEmail, MultipartFile file) {
        User user = requireUser(loginEmail);
        EmployeeEntity employee = requireEmployee(loginEmail);
        if (file == null || file.isEmpty()) {
            throw new RecruitmentNotificationException("Photo file is required.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new RecruitmentNotificationException("Photo must be 2 MB or smaller.");
        }

        EmployeeProfile profile = employeeProfileRepository.findByEmployeeEmployeeId(employee.getEmployeeId())
                .orElseGet(() -> {
                    EmployeeProfile created = new EmployeeProfile();
                    created.setEmployee(employee);
                    created.setCreatedBy(user.getEmail());
                    return created;
                });
        String previousPhotoPath = profile.getPhotoPath();

        FileUploadResult uploadResult;
        try {
            uploadResult = fileStorageService.store(file, EMPLOYEE_PHOTO_MODULE);
        } catch (FileStorageException ex) {
            throw new RecruitmentNotificationException(ex.getMessage());
        }

        profile.setPhotoPath(uploadResult.fullPath());
        profile.setUpdatedBy(user.getEmail());
        EmployeeProfile savedProfile = employeeProfileRepository.save(profile);
        if (StringUtils.hasText(previousPhotoPath) && !Objects.equals(previousPhotoPath, uploadResult.fullPath())) {
            fileStorageService.deleteQuietly(previousPhotoPath);
        }

        log.info("Employee profile photo uploaded for employeeId={} userId={}", employee.getEmployeeId(), user.getId());
        return toDto(savedProfile, user, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Path> resolveCurrentEmployeePhoto(String loginEmail) {
        requireUser(loginEmail);
        EmployeeEntity employee = requireEmployee(loginEmail);
        EmployeeProfile profile = employeeProfileRepository.findByEmployeeEmployeeId(employee.getEmployeeId()).orElse(null);
        String photoPath = resolvePhotoPath(profile, employee);
        if (!StringUtils.hasText(photoPath) || !fileStorageService.isManagedFileAllowed(photoPath, EMPLOYEE_PHOTO_MODULE)) {
            return Optional.empty();
        }
        return fileStorageService.resolveManagedPath(photoPath);
    }

    private User requireUser(String loginEmail) {
        if (!StringUtils.hasText(loginEmail)) {
            throw new RecruitmentNotificationException("Logged-in user is required.");
        }
        return userRepository.findByEmailIgnoreCaseAndActiveTrue(loginEmail.trim())
                .orElseThrow(() -> new RecruitmentNotificationException("Logged-in user account is not active."));
    }

    private Optional<EmployeeEntity> resolveEmployee(String loginEmail) {
        if (!StringUtils.hasText(loginEmail)) {
            return Optional.empty();
        }
        List<EmployeeEntity> employeeProfiles = employeeRepository.findDetailedProfilesByEmail(loginEmail.trim());
        if (employeeProfiles.size() > 1) {
            log.warn("Multiple employee master profiles found for loginEmail={}. Selecting first profile.", loginEmail);
        }
        return employeeProfiles.stream().findFirst();
    }

    private EmployeeEntity requireEmployee(String loginEmail) {
        return resolveEmployee(loginEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee master record was not found for the logged-in user."));
    }

    private EmployeeProfileDTO toDto(EmployeeProfile profile, User user, EmployeeEntity employee) {
        EmployeeProfileDTO dto = new EmployeeProfileDTO();
        dto.setProfileAvailable(profile != null);

        dto.setId(profile != null ? profile.getId() : null);
        dto.setEmployeeId(employee != null ? employee.getEmployeeId() : null);
        dto.setFullName(firstText(employee != null ? employee.getFullName() : null, user.getName()));
        dto.setDob(firstDate(profile != null ? normalizeDob(profile.getDob()) : null,
                employee != null ? normalizeDob(employee.getDateOfBirth()) : null));
        dto.setGender(firstText(profile != null ? normalizeText(profile.getGender()) : null,
                employee != null ? normalizeText(employee.getGender()) : null));
        dto.setAlternateMobileNo(profile != null ? profile.getAlternateMobileNo() : null);
        dto.setEmail(firstText(employee != null ? employee.getEmail() : null, user.getEmail()));
        dto.setPanNo(firstText(profile != null ? normalizePan(profile.getPanNo()) : null,
                employee != null ? normalizePanForDisplay(employee.getPanNumber()) : null));
        dto.setMaritalStatus(profile != null ? profile.getMaritalStatus() : null);
        dto.setBloodGroup(firstText(profile != null ? normalizeText(profile.getBloodGroup()) : null,
                employee != null ? normalizeText(employee.getBloodGroup()) : null));
        dto.setEmergencyContactName(firstText(profile != null ? normalizeText(profile.getEmergencyContactName()) : null,
                employee != null ? normalizeText(employee.getEmergencyContactName()) : null));
        dto.setEmergencyContactNo(firstText(profile != null ? normalizeText(profile.getEmergencyContactNo()) : null,
                employee != null ? normalizeText(employee.getEmergencyContactMobile()) : null));
        dto.setCurrentAddress(firstText(profile != null ? normalizeText(profile.getCurrentAddress()) : null,
                employee != null ? normalizeText(employee.getAddress()) : null));
        dto.setPermanentAddress(profile != null ? normalizeText(profile.getPermanentAddress()) : "");

        dto.setEmployeeCode(employee != null ? employee.getEmployeeCode() : "-");
        dto.setRole(resolveRole(user, employee));
        dto.setDepartment(resolveDepartment(employee, user));
        dto.setMobileNo(firstText(employee != null ? normalizeText(employee.getMobile()) : null, user.getMobileNo()));
        dto.setPhotoUrl(StringUtils.hasText(resolvePhotoPath(profile, employee)) ? cacheBustedEmployeePhotoUrl() : "");
        dto.setCompletionPercentage(calculateCompletionPercentage(dto));
        return dto;
    }

    private void syncEmployeeMaster(EmployeeEntity employee, EmployeeProfileDTO profileDTO, EmployeeProfile profile) {
        LocalDate dob = normalizeDob(profileDTO.getDob());
        if (dob != null) {
            employee.setDateOfBirth(dob);
        }
        setIfText(profile.getGender(), employee::setGender);
        setIfText(normalizeText(profileDTO.getMobileNo()), employee::setMobile);
        setIfText(profile.getPanNo(), employee::setPanNumber);
        setIfText(profile.getBloodGroup(), employee::setBloodGroup);
        setIfText(profile.getEmergencyContactName(), employee::setEmergencyContactName);
        setIfText(profile.getEmergencyContactNo(), employee::setEmergencyContactMobile);
        setIfText(profile.getAlternateMobileNo(), employee::setEmergencyContactAltMobile);
        setIfText(profile.getCurrentAddress(), employee::setAddress);
    }

    private String resolveRole(User user, EmployeeEntity employee) {
        if (employee != null && employee.getDesignation() != null
                && StringUtils.hasText(employee.getDesignation().getDesignationName())) {
            return employee.getDesignation().getDesignationName().trim();
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(role -> role.replace("ROLE_", "").replace('_', ' '))
                .findFirst()
                .orElse("Employee");
    }

    private String resolveDepartment(EmployeeEntity employee, User user) {
        if (employee != null && employee.getDepartmentRegistration() != null
                && StringUtils.hasText(employee.getDepartmentRegistration().getDepartmentName())) {
            return employee.getDepartmentRegistration().getDepartmentName().trim();
        }
        if (user.getDepartmentRegistrationId() != null
                && StringUtils.hasText(user.getDepartmentRegistrationId().getDepartmentName())) {
            return user.getDepartmentRegistrationId().getDepartmentName().trim();
        }
        return "-";
    }

    private int calculateCompletionPercentage(EmployeeProfileDTO dto) {
        List<String> values = Stream.of(
                dto.getFullName(),
                dto.getDob() != null ? dto.getDob().toString() : null,
                dto.getGender(),
                dto.getMobileNo(),
                dto.getAlternateMobileNo(),
                dto.getEmail(),
                dto.getPanNo(),
                dto.getMaritalStatus(),
                dto.getBloodGroup(),
                dto.getEmergencyContactName(),
                dto.getEmergencyContactNo(),
                dto.getCurrentAddress(),
                dto.getPermanentAddress(),
                dto.getPhotoUrl())
                .toList();
        long completed = values.stream().filter(StringUtils::hasText).count();
        return (int) Math.round((completed * 100.0d) / values.size());
    }

    private String resolvePhotoPath(EmployeeProfile profile, EmployeeEntity employee) {
        if (profile != null && StringUtils.hasText(profile.getPhotoPath())) {
            return profile.getPhotoPath();
        }
        return employee != null ? employee.getPhotoPath() : null;
    }

    private String cacheBustedEmployeePhotoUrl() {
        return "/employee/profile/photo?v=" + System.currentTimeMillis();
    }

    private LocalDate firstDate(LocalDate... values) {
        if (values == null) {
            return null;
        }
        for (LocalDate value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        String upperValue = normalized.toUpperCase(Locale.ROOT);
        if ("NOT_PROVIDED".equals(upperValue) || "NOT_SPECIFIED".equals(upperValue)) {
            return null;
        }
        return normalized;
    }

    private LocalDate normalizeDob(LocalDate value) {
        if (value == null || DEFAULT_DOB.equals(value)) {
            return null;
        }
        return value;
    }

    private String normalizePan(String value) {
        String normalizedText = normalizeText(value);
        if (!StringUtils.hasText(normalizedText)) {
            return null;
        }
        String normalized = normalizedText.toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!normalized.matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
            throw new RecruitmentNotificationException("PAN must match ABCDE1234F format.");
        }
        return normalized;
    }

    private String normalizePanForDisplay(String value) {
        String normalizedText = normalizeText(value);
        if (!StringUtils.hasText(normalizedText)) {
            return null;
        }
        String normalized = normalizedText.toUpperCase(Locale.ROOT);
        return normalized.matches("^[A-Z]{5}[0-9]{4}[A-Z]$") ? normalized : null;
    }

    private void setIfText(String value, java.util.function.Consumer<String> setter) {
        if (StringUtils.hasText(value)) {
            setter.accept(value);
        }
    }
}
