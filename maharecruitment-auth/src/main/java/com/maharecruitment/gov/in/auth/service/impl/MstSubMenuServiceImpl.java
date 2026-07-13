package com.maharecruitment.gov.in.auth.service.impl;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.entity.MstSubMenu;
import com.maharecruitment.gov.in.auth.repository.MstSubMenuRepository;
import com.maharecruitment.gov.in.auth.service.MstSubMenuService;

@Service
public class MstSubMenuServiceImpl implements MstSubMenuService {

    private static final Comparator<MstSubMenu> SUB_MENU_ORDER = Comparator
            .comparing((MstSubMenu subMenu) -> subMenu.getMenu() != null ? subMenu.getMenu().getMenuId() : null,
                    Comparator.nullsLast(Long::compareTo))
            .thenComparing(MstSubMenu::getSubMenuId, Comparator.nullsLast(Long::compareTo));

    private final MstSubMenuRepository mstSubMenuRepository;

    public MstSubMenuServiceImpl(MstSubMenuRepository mstSubMenuRepository) {
        this.mstSubMenuRepository = mstSubMenuRepository;
    }

    @Override
    public List<MstSubMenu> getAllSubMenus() {
        return mstSubMenuRepository.findAllByOrderByMenuMenuIdAscSubMenuIdAsc();
    }

    @Override
    public List<MstSubMenu> getSubMenusByMenuIdsAndRoleIds(List<Long> menuIds, List<Long> roleIds) {
        if (menuIds == null || menuIds.isEmpty() || roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return mstSubMenuRepository.findVisibleSubMenusByMenuIdsAndRoleIds(menuIds, roleIds)
                .stream()
                .filter(Objects::nonNull)
                .filter(subMenu -> subMenu.getIsActive() == null || Character.toUpperCase(subMenu.getIsActive()) == 'Y')
                .sorted(SUB_MENU_ORDER)
                .toList();
    }

    @Override
    public List<MstSubMenu> getSubMenusByMenuIdsAndRoleNames(List<Long> menuIds, List<String> roleNames) {
        List<String> roleCandidates = resolveRoleCandidates(roleNames);
        if (menuIds == null || menuIds.isEmpty() || roleCandidates.isEmpty()) {
            return List.of();
        }
        return mstSubMenuRepository.findVisibleSubMenusByMenuIdsAndRoleNames(menuIds, roleCandidates)
                .stream()
                .filter(Objects::nonNull)
                .filter(subMenu -> subMenu.getIsActive() == null || Character.toUpperCase(subMenu.getIsActive()) == 'Y')
                .sorted(SUB_MENU_ORDER)
                .toList();
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
