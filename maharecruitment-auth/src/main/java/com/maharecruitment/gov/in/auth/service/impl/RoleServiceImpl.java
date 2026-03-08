package com.maharecruitment.gov.in.auth.service.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role getByName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }

        for (String candidate : resolveRoleCandidates(roleName.trim())) {
            Role role = roleRepository.findByNameIgnoreCase(candidate).orElse(null);
            if (role != null) {
                return role;
            }
        }

        return null;
    }

    private Set<String> resolveRoleCandidates(String roleName) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(roleName);

        if (roleName.startsWith("ROLE_")) {
            candidates.add(roleName.substring(5));
        } else {
            candidates.add("ROLE_" + roleName);
        }

        candidates.add(roleName.toUpperCase());
        if (roleName.startsWith("ROLE_")) {
            candidates.add(roleName.substring(5).toUpperCase());
        }

        return candidates;
    }
}
