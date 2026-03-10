package com.maharecruitment.gov.in.auth.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.dto.SubMenuUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.MstSubMenu;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.MstSubMenuRepository;
import com.maharecruitment.gov.in.auth.service.SubMenuManagementService;

@Service
@Transactional
public class SubMenuManagementServiceImpl implements SubMenuManagementService {

    private static final Logger log = LoggerFactory.getLogger(SubMenuManagementServiceImpl.class);

    private static final int MENU_PARENT = 0;

    private final MstSubMenuRepository mstSubMenuRepository;
    private final MstMenuRepository mstMenuRepository;

    public SubMenuManagementServiceImpl(
            MstSubMenuRepository mstSubMenuRepository,
            MstMenuRepository mstMenuRepository) {
        this.mstSubMenuRepository = mstSubMenuRepository;
        this.mstMenuRepository = mstMenuRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MstSubMenu> getAll(Pageable pageable) {
        return mstSubMenuRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public MstSubMenu getById(Long subMenuId) {
        return mstSubMenuRepository.findById(subMenuId)
                .orElseThrow(() -> new IllegalArgumentException("Submenu not found for id: " + subMenuId));
    }

    @Override
    public MstSubMenu create(SubMenuUpsertRequest request) {
        ValidatedSubMenu validated = validateForCreate(request);

        MstSubMenu entity = new MstSubMenu();
        apply(entity, validated);
        MstSubMenu saved = mstSubMenuRepository.save(entity);
        log.info("Submenu created: id={}, name={}, url={}",
                saved.getSubMenuId(),
                saved.getSubMenuNameEnglish(),
                saved.getUrl());
        return saved;
    }

    @Override
    public MstSubMenu update(Long subMenuId, SubMenuUpsertRequest request) {
        MstSubMenu existing = getById(subMenuId);
        ValidatedSubMenu validated = validateForUpdate(subMenuId, request);

        apply(existing, validated);
        MstSubMenu saved = mstSubMenuRepository.save(existing);
        log.info("Submenu updated: id={}, name={}, url={}",
                saved.getSubMenuId(),
                saved.getSubMenuNameEnglish(),
                saved.getUrl());
        return saved;
    }

    @Override
    public void delete(Long subMenuId) {
        MstSubMenu existing = getById(subMenuId);
        mstSubMenuRepository.delete(existing);
        log.info("Submenu deleted: id={}, name={}", existing.getSubMenuId(), existing.getSubMenuNameEnglish());
    }

    private void apply(MstSubMenu entity, ValidatedSubMenu validated) {
        entity.setMenu(validated.menu());
        entity.setSubMenuNameEnglish(validated.subMenuNameEnglish());
        entity.setSubMenuNameMarathi(validated.subMenuNameMarathi());
        entity.setControllerName(validated.controllerName());
        entity.setUrl(validated.url());
        entity.setIcon(validated.icon());
        entity.setIsActive(validated.isActive());
    }

    private ValidatedSubMenu validateForCreate(SubMenuUpsertRequest request) {
        MstMenu menu = validateParentMenu(request.getMenuId());
        String nameEnglish = normalizeRequired(request.getSubMenuNameEnglish(), "Submenu name (English)");
        String normalizedUrl = normalizeRequired(request.getUrl(), "Submenu URL");

        if (mstSubMenuRepository.existsByMenuMenuIdAndSubMenuNameEnglishIgnoreCase(menu.getMenuId(), nameEnglish)) {
            throw new IllegalArgumentException("Submenu name already exists under selected menu: " + nameEnglish);
        }
        if (mstSubMenuRepository.existsByUrlIgnoreCase(normalizedUrl)
                || mstMenuRepository.existsByUrlIgnoreCase(normalizedUrl)) {
            throw new IllegalArgumentException("URL already exists: " + normalizedUrl);
        }

        String marathiName = normalizeOptional(request.getSubMenuNameMarathi());
        String controllerName = normalizeOptional(request.getControllerName());
        String icon = normalizeOptional(request.getIcon());
        Character isActive = normalizeActiveFlag(request.getIsActive());

        return new ValidatedSubMenu(
                menu,
                nameEnglish,
                marathiName != null ? marathiName : nameEnglish,
                controllerName != null ? controllerName : nameEnglish,
                normalizedUrl,
                icon,
                isActive);
    }

    private ValidatedSubMenu validateForUpdate(Long subMenuId, SubMenuUpsertRequest request) {
        MstMenu menu = validateParentMenu(request.getMenuId());
        String nameEnglish = normalizeRequired(request.getSubMenuNameEnglish(), "Submenu name (English)");
        String normalizedUrl = normalizeRequired(request.getUrl(), "Submenu URL");

        if (mstSubMenuRepository.existsByMenuMenuIdAndSubMenuNameEnglishIgnoreCaseAndSubMenuIdNot(
                menu.getMenuId(),
                nameEnglish,
                subMenuId)) {
            throw new IllegalArgumentException("Submenu name already exists under selected menu: " + nameEnglish);
        }
        if (mstSubMenuRepository.existsByUrlIgnoreCaseAndSubMenuIdNot(normalizedUrl, subMenuId)
                || mstMenuRepository.existsByUrlIgnoreCase(normalizedUrl)) {
            throw new IllegalArgumentException("URL already exists: " + normalizedUrl);
        }

        String marathiName = normalizeOptional(request.getSubMenuNameMarathi());
        String controllerName = normalizeOptional(request.getControllerName());
        String icon = normalizeOptional(request.getIcon());
        Character isActive = normalizeActiveFlag(request.getIsActive());

        return new ValidatedSubMenu(
                menu,
                nameEnglish,
                marathiName != null ? marathiName : nameEnglish,
                controllerName != null ? controllerName : nameEnglish,
                normalizedUrl,
                icon,
                isActive);
    }

    private MstMenu validateParentMenu(Long menuId) {
        if (menuId == null) {
            throw new IllegalArgumentException("Parent menu is required.");
        }

        MstMenu menu = mstMenuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found for id: " + menuId));
        if (menu.getIsSubMenu() == null || menu.getIsSubMenu() != MENU_PARENT) {
            throw new IllegalArgumentException("Submenu can be created only under parent menu.");
        }
        return menu;
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

    private Character normalizeActiveFlag(Character activeFlag) {
        if (activeFlag == null) {
            return 'Y';
        }
        char normalized = Character.toUpperCase(activeFlag);
        if (normalized != 'Y' && normalized != 'N') {
            throw new IllegalArgumentException("Status must be Y or N.");
        }
        return normalized;
    }

    private record ValidatedSubMenu(
            MstMenu menu,
            String subMenuNameEnglish,
            String subMenuNameMarathi,
            String controllerName,
            String url,
            String icon,
            Character isActive) {
    }
}
