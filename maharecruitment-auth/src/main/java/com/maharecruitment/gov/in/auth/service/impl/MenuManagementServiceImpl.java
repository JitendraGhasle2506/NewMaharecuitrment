package com.maharecruitment.gov.in.auth.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.MenuUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.MstSubMenuRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.service.MenuManagementService;

@Service
@Transactional
public class MenuManagementServiceImpl implements MenuManagementService {

    private static final Logger log = LoggerFactory.getLogger(MenuManagementServiceImpl.class);

    private static final int MENU_PARENT = 0;
    private static final int MENU_DIRECT = 1;

    private final MstMenuRepository mstMenuRepository;
    private final MstSubMenuRepository mstSubMenuRepository;
    private final RoleRepository roleRepository;

    public MenuManagementServiceImpl(
            MstMenuRepository mstMenuRepository,
            MstSubMenuRepository mstSubMenuRepository,
            RoleRepository roleRepository) {
        this.mstMenuRepository = mstMenuRepository;
        this.mstSubMenuRepository = mstSubMenuRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MstMenu> getAll(Pageable pageable) {
        return mstMenuRepository.findAllWithRoles(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MstMenu> getParentMenus() {
        return mstMenuRepository.findByIsSubMenuAndIsActiveIgnoreCaseOrderByMenuNameEnglishAsc(MENU_PARENT, "Y");
    }

    @Override
    @Transactional(readOnly = true)
    public MstMenu getById(Long menuId) {
        return mstMenuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found for id: " + menuId));
    }

    @Override
    public MstMenu create(MenuUpsertRequest request) {
        ValidatedMenu validated = validateForCreate(request);

        MstMenu entity = new MstMenu();
        apply(entity, validated);
        MstMenu saved = mstMenuRepository.save(entity);
        log.info("Menu created: id={}, name={}, url={}", saved.getMenuId(), saved.getMenuNameEnglish(), saved.getUrl());
        return saved;
    }

    @Override
    public MstMenu update(Long menuId, MenuUpsertRequest request) {
        MstMenu existing = getById(menuId);
        ValidatedMenu validated = validateForUpdate(menuId, request);

        apply(existing, validated);
        MstMenu saved = mstMenuRepository.save(existing);
        log.info("Menu updated: id={}, name={}, url={}", saved.getMenuId(), saved.getMenuNameEnglish(), saved.getUrl());
        return saved;
    }

    @Override
    public void delete(Long menuId) {
        MstMenu existing = getById(menuId);
        long childSubMenuCount = mstSubMenuRepository.countByMenuMenuId(menuId);
        if (childSubMenuCount > 0) {
            throw new IllegalArgumentException("Cannot delete menu. Remove its submenus first.");
        }

        if (existing.getRoles() != null) {
            existing.getRoles().clear();
            mstMenuRepository.save(existing);
        }

        mstMenuRepository.delete(existing);
        log.info("Menu deleted: id={}, name={}", existing.getMenuId(), existing.getMenuNameEnglish());
    }

    private void apply(MstMenu entity, ValidatedMenu validated) {
        entity.setMenuNameEnglish(validated.menuNameEnglish());
        entity.setMenuNameMarathi(validated.menuNameMarathi());
        entity.setIcon(validated.icon());
        entity.setUrl(validated.url());
        entity.setIsSubMenu(validated.isSubMenu());
        entity.setIsActive(validated.isActive());
        entity.setRoles(validated.roles());
    }

    private ValidatedMenu validateForCreate(MenuUpsertRequest request) {
        String menuNameEnglish = normalizeRequired(request.getMenuNameEnglish(), "Menu name (English)");
        Integer isSubMenu = normalizeMenuType(request.getIsSubMenu());
        String normalizedUrl = normalizeUrlByType(request.getUrl(), isSubMenu);

        if (mstMenuRepository.existsByMenuNameEnglishIgnoreCase(menuNameEnglish)) {
            throw new IllegalArgumentException("Menu name already exists: " + menuNameEnglish);
        }
        ensureUniqueUrlForCreate(normalizedUrl);

        Set<Role> roles = resolveRoles(request.getRoleIds());
        String isActive = normalizeActiveFlag(request.getIsActive());
        String menuNameMarathi = normalizeOptional(request.getMenuNameMarathi());
        String icon = normalizeOptional(request.getIcon());

        return new ValidatedMenu(
                menuNameEnglish,
                menuNameMarathi != null ? menuNameMarathi : menuNameEnglish,
                icon,
                normalizedUrl,
                isSubMenu,
                isActive,
                roles);
    }

    private ValidatedMenu validateForUpdate(Long menuId, MenuUpsertRequest request) {
        String menuNameEnglish = normalizeRequired(request.getMenuNameEnglish(), "Menu name (English)");
        Integer isSubMenu = normalizeMenuType(request.getIsSubMenu());
        String normalizedUrl = normalizeUrlByType(request.getUrl(), isSubMenu);

        if (mstMenuRepository.existsByMenuNameEnglishIgnoreCaseAndMenuIdNot(menuNameEnglish, menuId)) {
            throw new IllegalArgumentException("Menu name already exists: " + menuNameEnglish);
        }
        ensureUniqueUrlForUpdate(normalizedUrl, menuId);

        Set<Role> roles = resolveRoles(request.getRoleIds());
        String isActive = normalizeActiveFlag(request.getIsActive());
        String menuNameMarathi = normalizeOptional(request.getMenuNameMarathi());
        String icon = normalizeOptional(request.getIcon());

        return new ValidatedMenu(
                menuNameEnglish,
                menuNameMarathi != null ? menuNameMarathi : menuNameEnglish,
                icon,
                normalizedUrl,
                isSubMenu,
                isActive,
                roles);
    }

    private void ensureUniqueUrlForCreate(String normalizedUrl) {
        if (normalizedUrl == null) {
            return;
        }
        if (mstMenuRepository.existsByUrlIgnoreCase(normalizedUrl)
                || mstSubMenuRepository.existsByUrlIgnoreCase(normalizedUrl)) {
            throw new IllegalArgumentException("URL already exists: " + normalizedUrl);
        }
    }

    private void ensureUniqueUrlForUpdate(String normalizedUrl, Long menuId) {
        if (normalizedUrl == null) {
            return;
        }
        if (mstMenuRepository.existsByUrlIgnoreCaseAndMenuIdNot(normalizedUrl, menuId)
                || mstSubMenuRepository.existsByUrlIgnoreCase(normalizedUrl)) {
            throw new IllegalArgumentException("URL already exists: " + normalizedUrl);
        }
    }

    private Set<Role> resolveRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }

        List<Long> normalizedRoleIds = roleIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (normalizedRoleIds.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }

        List<Role> roles = roleRepository.findAllById(normalizedRoleIds);
        if (roles.size() != normalizedRoleIds.size()) {
            throw new IllegalArgumentException("One or more selected roles are invalid.");
        }
        return new LinkedHashSet<>(roles);
    }

    private Integer normalizeMenuType(Integer isSubMenu) {
        if (isSubMenu == null || (isSubMenu != MENU_PARENT && isSubMenu != MENU_DIRECT)) {
            throw new IllegalArgumentException("Menu type is invalid.");
        }
        return isSubMenu;
    }

    private String normalizeUrlByType(String url, Integer isSubMenu) {
        String normalizedUrl = normalizeOptional(url);
        if (isSubMenu == MENU_PARENT) {
            return null;
        }
        if (normalizedUrl == null) {
            throw new IllegalArgumentException("URL is required for direct-link menu.");
        }
        return normalizedUrl;
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeActiveFlag(String activeFlag) {
        if (activeFlag == null || activeFlag.isBlank()) {
            return "Y";
        }
        String normalized = activeFlag.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException("Status must be Y or N.");
        }
        return normalized;
    }

    private record ValidatedMenu(
            String menuNameEnglish,
            String menuNameMarathi,
            String icon,
            String url,
            Integer isSubMenu,
            String isActive,
            Set<Role> roles) {
    }
}
