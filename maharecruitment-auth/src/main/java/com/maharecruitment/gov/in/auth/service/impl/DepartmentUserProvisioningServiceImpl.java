package com.maharecruitment.gov.in.auth.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningResult;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.DepartmentUserProvisioningService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.util.SecurePasswordGenerator;

@Service
@Transactional
public class DepartmentUserProvisioningServiceImpl implements DepartmentUserProvisioningService {

    private static final String DEPARTMENT_ROLE = "ROLE_DEPARTMENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAffiliationService userAffiliationService;

    public DepartmentUserProvisioningServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UserAffiliationService userAffiliationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAffiliationService = userAffiliationService;
    }

    @Override
    public DepartmentUserProvisioningResult createDepartmentUser(DepartmentUserProvisioningRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email == null) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("A user account already exists for the primary email address.");
        }

        Role departmentRole = roleRepository.findByNameIgnoreCase(DEPARTMENT_ROLE)
                .orElseGet(() -> roleRepository.save(new Role(null, DEPARTMENT_ROLE, new ArrayList<>(), new ArrayList<>())));

        String temporaryPassword = SecurePasswordGenerator.generate(12);

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setMobileNo(request.getMobileNo().trim());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setDepartmentRegistrationId(request.getDepartmentRegistration());
        user.setRoles(List.of(departmentRole));

        User savedUser = userRepository.save(user);
        userAffiliationService.synchronizeUserProfile(savedUser);
        userAffiliationService.synchronizePrimaryDepartment(savedUser, request.getDepartmentRegistration());

        return DepartmentUserProvisioningResult.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
