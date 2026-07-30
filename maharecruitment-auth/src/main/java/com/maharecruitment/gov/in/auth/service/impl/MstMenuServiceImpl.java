package com.maharecruitment.gov.in.auth.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.service.MstMenuService;

@Service
public class MstMenuServiceImpl implements MstMenuService {

    private final MstMenuRepository mstMenuRepository;

    public MstMenuServiceImpl(MstMenuRepository mstMenuRepository) {
        this.mstMenuRepository = mstMenuRepository;
    }

    @Override
    public List<MstMenu> findMenusByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return mstMenuRepository.findMenusByRoleIds(roleIds);
    }

    @Override
    public List<MstMenu> findMenusByRoleNames(List<String> roleNames) {
        List<String> roleCandidates = resolveRoleCandidates(roleNames);
        if (roleCandidates.isEmpty()) {
            return List.of();
        }
        return mstMenuRepository.findMenusByRoleNames(roleCandidates);
    }

    private List<String> resolveRoleCandidates(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        Set<String> candidates = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            if (roleName == null || roleName.isBlank()) {
                continue;
            }

            String normalized = roleName.trim().toUpperCase(Locale.ROOT);
            candidates.add(normalized);
            if (normalized.startsWith("ROLE_")) {
                candidates.add(normalized.substring(5));
            } else {
                candidates.add("ROLE_" + normalized);
            }
        }
        return List.copyOf(candidates);
    }
}
