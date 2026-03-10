package com.maharecruitment.gov.in.auth.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.UserUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserManagementService;

@Service
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + id));
    }

    @Override
    public User create(UserUpsertRequest request) {
        validateForCreate(request);

        User user = new User();
        applyCommonFields(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));

        User saved = userRepository.save(user);
        log.info("User created: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Override
    public User update(Long id, UserUpsertRequest request) {
        User existing = getById(id);
        validateForUpdate(existing.getId(), request);

        applyCommonFields(existing, request);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        User saved = userRepository.save(existing);
        log.info("User updated: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Override
    public void delete(Long id) {
        User existing = getById(id);
        userRepository.delete(existing);
        log.info("User deleted: id={}, email={}", existing.getId(), existing.getEmail());
    }

    private void applyCommonFields(User user, UserUpsertRequest request) {
        user.setName(normalizeRequired(request.getName(), "User name"));
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setMobileNo(normalizeOptional(request.getMobileNo()));
        user.setRoles(resolveRoles(request.getRoleIds()));
        user.setDepartmentRegistrationId(resolveDepartment(request.getDepartmentRegistrationId()));
    }

    private void validateForCreate(UserUpsertRequest request) {
        validateCommon(request, null);
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    private void validateForUpdate(Long id, UserUpsertRequest request) {
        validateCommon(request, id);
    }

    private void validateCommon(UserUpsertRequest request, Long id) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        boolean duplicateEmail = (id == null)
                ? userRepository.existsByEmailIgnoreCase(normalizedEmail)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id);
        if (duplicateEmail) {
            throw new IllegalArgumentException("Email already exists: " + normalizedEmail);
        }

        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }
    }

    private List<Role> resolveRoles(List<Long> roleIds) {
        List<Long> ids = new ArrayList<>(roleIds);
        List<Role> roles = roleRepository.findAllById(ids);
        if (roles.size() != ids.size()) {
            throw new IllegalArgumentException("One or more selected roles are invalid.");
        }
        return roles;
    }

    private DepartmentRegistrationEntity resolveDepartment(Long departmentRegistrationId) {
        if (departmentRegistrationId == null) {
            return null;
        }
        return departmentRegistrationRepository.findById(departmentRegistrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department registration not found for id: " + departmentRegistrationId));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        return email.trim().toLowerCase();
    }
}
