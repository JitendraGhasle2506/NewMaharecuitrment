package com.maharecruitment.gov.in.auth.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningRequest;
import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningResult;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.AgencyUserProvisioningService;
import com.maharecruitment.gov.in.auth.util.SecurePasswordGenerator;

@Service
@Transactional
public class AgencyUserProvisioningServiceImpl implements AgencyUserProvisioningService {

    private static final String AGENCY_ROLE = "ROLE_AGENCY";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AgencyUserProvisioningServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AgencyUserProvisioningResult createOrSyncAgencyUser(AgencyUserProvisioningRequest request) {
        String email = normalizeEmail(request.getEmail());
        String previousEmail = normalizeEmail(request.getPreviousEmail());
        Role agencyRole = resolveAgencyRole();

        User existingUser = previousEmail == null
                ? null
                : userRepository.findByEmailIgnoreCase(previousEmail).orElse(null);

        if (existingUser == null) {
            return createAgencyUser(request, email, agencyRole);
        }

        if (!previousEmail.equals(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("A user account already exists for the updated agency official email.");
        }

        existingUser.setName(request.getName().trim());
        existingUser.setEmail(email);
        existingUser.setMobileNo(request.getMobileNo().trim());
        ensureRole(existingUser, agencyRole);

        User savedUser = userRepository.save(existingUser);
        return AgencyUserProvisioningResult.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .created(false)
                .build();
    }

    private AgencyUserProvisioningResult createAgencyUser(
            AgencyUserProvisioningRequest request,
            String email,
            Role agencyRole) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("A user account already exists for the agency official email.");
        }

        String temporaryPassword = SecurePasswordGenerator.generate(12);

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setMobileNo(request.getMobileNo().trim());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setRoles(List.of(agencyRole));

        User savedUser = userRepository.save(user);

        return AgencyUserProvisioningResult.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .temporaryPassword(temporaryPassword)
                .created(true)
                .build();
    }

    private Role resolveAgencyRole() {
        return roleRepository.findByNameIgnoreCase(AGENCY_ROLE)
                .orElseGet(() -> roleRepository.save(new Role(null, AGENCY_ROLE, new ArrayList<>(), new ArrayList<>())));
    }

    private void ensureRole(User user, Role role) {
        List<Role> roles = user.getRoles() == null ? new ArrayList<>() : new ArrayList<>(user.getRoles());
        boolean present = roles.stream()
                .anyMatch(existingRole -> existingRole.getName().equalsIgnoreCase(role.getName()));
        if (!present) {
            roles.add(role);
            user.setRoles(roles);
        }
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
