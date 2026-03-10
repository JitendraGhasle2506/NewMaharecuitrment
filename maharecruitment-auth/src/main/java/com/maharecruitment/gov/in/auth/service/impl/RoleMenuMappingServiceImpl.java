package com.maharecruitment.gov.in.auth.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.service.RoleMenuMappingService;

@Service
@Transactional
public class RoleMenuMappingServiceImpl implements RoleMenuMappingService {

    private static final Logger log = LoggerFactory.getLogger(RoleMenuMappingServiceImpl.class);

    private final RoleRepository roleRepository;
    private final MstMenuRepository mstMenuRepository;

    public RoleMenuMappingServiceImpl(
            RoleRepository roleRepository,
            MstMenuRepository mstMenuRepository) {
        this.roleRepository = roleRepository;
        this.mstMenuRepository = mstMenuRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MstMenu> getAllMenus() {
        return mstMenuRepository.findByIsActiveIgnoreCaseOrderByMenuNameEnglishAsc("Y");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MstMenu> getMenusByRoleId(Long roleId) {
        validateRole(roleId);
        return mstMenuRepository.findMenusByRoleId(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return getMenusByRoleId(roleId).stream()
                .map(MstMenu::getMenuId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countMenusByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedRoleIds = roleIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (normalizedRoleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = mstMenuRepository.countMenusByRoleIds(normalizedRoleIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()));

        return normalizedRoleIds.stream()
                .collect(Collectors.toMap(Function.identity(), roleId -> counts.getOrDefault(roleId, 0L)));
    }

    @Override
    public void replaceRoleMenuMappings(Long roleId, List<Long> menuIds) {
        Role role = validateRole(roleId);
        List<Long> normalizedMenuIds = normalizeMenuIds(menuIds);

        List<MstMenu> currentlyMappedMenus = mstMenuRepository.findMenusByRoleId(roleId);
        List<MstMenu> requestedMenus = normalizedMenuIds.isEmpty()
                ? List.of()
                : mstMenuRepository.findAllById(normalizedMenuIds);

        if (requestedMenus.size() != normalizedMenuIds.size()) {
            throw new IllegalArgumentException("One or more selected menus are invalid.");
        }

        Set<Long> requestedSet = new LinkedHashSet<>(normalizedMenuIds);
        List<MstMenu> toSave = new ArrayList<>();

        for (MstMenu menu : currentlyMappedMenus) {
            if (!requestedSet.contains(menu.getMenuId()) && menu.getRoles().remove(role)) {
                toSave.add(menu);
            }
        }

        for (MstMenu menu : requestedMenus) {
            if (menu.getRoles().add(role)) {
                toSave.add(menu);
            }
        }

        if (!toSave.isEmpty()) {
            mstMenuRepository.saveAll(toSave);
        }

        log.info("Role-menu mapping updated: roleId={}, mappedMenus={}", roleId, requestedSet.size());
    }

    @Override
    public void clearRoleMenuMappings(Long roleId) {
        Role role = validateRole(roleId);
        List<MstMenu> mappedMenus = mstMenuRepository.findMenusByRoleId(roleId);
        if (mappedMenus.isEmpty()) {
            return;
        }

        for (MstMenu menu : mappedMenus) {
            menu.getRoles().remove(role);
        }
        mstMenuRepository.saveAll(mappedMenus);

        log.info("Role-menu mappings cleared for roleId={}", roleId);
    }

    private Role validateRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found for id: " + roleId));
    }

    private List<Long> normalizeMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        return menuIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }
}
