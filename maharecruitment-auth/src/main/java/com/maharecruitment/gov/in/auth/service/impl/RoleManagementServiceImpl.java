package com.maharecruitment.gov.in.auth.service.impl;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.service.RoleManagementService;

@Service
@Transactional
public class RoleManagementServiceImpl implements RoleManagementService {

    private static final Logger log = LoggerFactory.getLogger(RoleManagementServiceImpl.class);
    private static final Set<String> CANONICAL_ROLE_NAMES = Set.of(
            "ROLE_DEPARTMENT",
            "ROLE_HR",
            "ROLE_AGENCY",
            "ROLE_ADMIN",
            "ROLE_USER",
            "ROLE_STM",
            "ROLE_HOD",
            "ROLE_COO",
            "ROLE_PM",
            "ROLE_AUDITOR",
            "ROLE_EMPLOYEE");

    private final RoleRepository roleRepository;

    public RoleManagementServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Role> getAll(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAll() {
        return roleRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found for id: " + id));
    }

    @Override
    public Role create(String name) {
        String normalizedName = normalizeRoleName(name);
        if (roleRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Role already exists: " + normalizedName);
        }

        Role role = new Role();
        role.setName(normalizedName);
        Role saved = roleRepository.save(role);
        log.info("Role created: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    public Role update(Long id, String name) {
        Role existing = getById(id);
        String normalizedName = normalizeRoleName(name);

        if (roleRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new IllegalArgumentException("Role already exists: " + normalizedName);
        }

        existing.setName(normalizedName);
        Role saved = roleRepository.save(existing);
        log.info("Role updated: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    public void delete(Long id) {
        Role existing = getById(id);
        if (CANONICAL_ROLE_NAMES.contains(existing.getName())) {
            throw new IllegalArgumentException("Canonical system roles cannot be deleted.");
        }
        if (existing.getUsers() != null && !existing.getUsers().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete role mapped to users.");
        }

        roleRepository.delete(existing);
        log.info("Role deleted: id={}, name={}", existing.getId(), existing.getName());
    }

    private String normalizeRoleName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name is required.");
        }

        String normalizedName = name.trim().toUpperCase();
        if (!CANONICAL_ROLE_NAMES.contains(normalizedName)) {
            throw new IllegalArgumentException("Only canonical ROLE_* names are allowed.");
        }

        return normalizedName;
    }
}
