package com.maharecruitment.gov.in.auth.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.MstSubMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.MstSubMenuRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;

@Component
@Order(20)
public class MenuBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuBootstrapInitializer.class);

    private final MstMenuRepository mstMenuRepository;
    private final MstSubMenuRepository mstSubMenuRepository;
    private final RoleRepository roleRepository;

    @Value("${app.bootstrap.menu.enabled:true}")
    private boolean enabled;

    public MenuBootstrapInitializer(
            MstMenuRepository mstMenuRepository,
            MstSubMenuRepository mstSubMenuRepository,
            RoleRepository roleRepository) {
        this.mstMenuRepository = mstMenuRepository;
        this.mstSubMenuRepository = mstSubMenuRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Menu bootstrap is disabled.");
            return;
        }

        Set<Role> adminRoles = resolveRolesWithFallback("ADMIN", "ADMIN", "ROLE_ADMIN");
        Set<Role> hrRoles = resolveRolesWithFallback("ROLE_HR", "HR", "ROLE_HR");

        MstMenu adminMenu = upsertMenu(
                "Administration",
                null,
                "fa fa-user-shield",
                0,
                adminRoles.toArray(new Role[0]));

        upsertSubMenu(adminMenu, "Admin Dashboard", "/admin/dashboard", "fa fa-gauge");
        upsertSubMenu(adminMenu, "Role Management", "/admin/roles", "fa fa-user-tag");
        upsertSubMenu(adminMenu, "User Management", "/admin/users", "fa fa-users-cog");
        upsertSubMenu(adminMenu, "Role Menu Mapping", "/admin/role-menu-mappings", "fa fa-diagram-project");
        upsertSubMenu(adminMenu, "Menu Management", "/admin/menus", "fa fa-bars");
        upsertSubMenu(adminMenu, "Submenu Management", "/admin/submenus", "fa fa-sitemap");

        MstMenu masterMenu = upsertMenu(
                "Master Management",
                null,
                "fa fa-database",
                0,
                mergeRoles(adminRoles, hrRoles).toArray(new Role[0]));

        upsertSubMenu(masterMenu, "Designation Master", "/master/designations", "fa fa-id-badge");
        upsertSubMenu(masterMenu, "Resource Levels", "/master/resource-levels", "fa fa-layer-group");
        upsertSubMenu(masterMenu, "Designation Rates", "/master/designation-rates", "fa fa-coins");

        Role departmentRole = roleRepository.findByNameIgnoreCase("ROLE_DEPARTMENT")
                .or(() -> roleRepository.findByNameIgnoreCase("DEPARTMENT"))
                .orElseGet(() -> createRoleIfMissing("ROLE_DEPARTMENT"));

        MstMenu departmentMenu = upsertMenu(
                "Department Module",
                null,
                "fa fa-building",
                0,
                departmentRole);

        upsertSubMenu(departmentMenu, "Department Dashboard", "/department/home", "fa fa-gauge");
        upsertSubMenu(departmentMenu, "Department Profile", "/department/profile", "fa fa-id-card");
        upsertSubMenu(departmentMenu, "Manpower Applications", "/department/manpower/list", "fa fa-users-gear");
        upsertSubMenu(departmentMenu, "New Manpower Application", "/department/manpower/apply", "fa fa-file-circle-plus");
    }

    private MstMenu upsertMenu(
            String menuNameEnglish,
            String url,
            String icon,
            Integer isSubMenu,
            Role... rolesToAssign) {
        MstMenu menu = mstMenuRepository.findByMenuNameEnglishIgnoreCase(menuNameEnglish)
                .orElseGet(MstMenu::new);

        menu.setMenuNameEnglish(menuNameEnglish);
        menu.setMenuNameMarathi(menuNameEnglish);
        menu.setUrl(url);
        menu.setIcon(icon);
        menu.setIsSubMenu(isSubMenu);
        menu.setIsActive("Y");

        Set<Role> roles = menu.getRoles();
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        if (rolesToAssign != null) {
            for (Role role : rolesToAssign) {
                if (role != null) {
                    roles.add(role);
                }
            }
        }
        menu.setRoles(roles);

        MstMenu saved = mstMenuRepository.save(menu);
        log.info("Menu bootstrapped: id={}, name={}", saved.getMenuId(), saved.getMenuNameEnglish());
        return saved;
    }

    private MstSubMenu upsertSubMenu(MstMenu menu, String name, String url, String icon) {
        MstSubMenu subMenu = mstSubMenuRepository
                .findByMenuMenuIdAndSubMenuNameEnglishIgnoreCase(menu.getMenuId(), name)
                .orElseGet(MstSubMenu::new);

        subMenu.setMenu(menu);
        subMenu.setSubMenuNameEnglish(name);
        subMenu.setSubMenuNameMarathi(name);
        subMenu.setControllerName(name);
        subMenu.setUrl(url);
        subMenu.setIcon(icon);
        subMenu.setIsActive('Y');

        MstSubMenu saved = mstSubMenuRepository.save(subMenu);
        log.info("Sub-menu bootstrapped: id={}, name={}, menuId={}",
                saved.getSubMenuId(),
                saved.getSubMenuNameEnglish(),
                menu.getMenuId());
        return saved;
    }

    private Set<Role> resolveRolesWithFallback(String createIfMissing, String... lookupNames) {
        Set<Role> resolvedRoles = new LinkedHashSet<>();
        if (lookupNames != null) {
            for (String roleName : lookupNames) {
                if (roleName == null || roleName.isBlank()) {
                    continue;
                }
                roleRepository.findByNameIgnoreCase(roleName.trim())
                        .ifPresent(resolvedRoles::add);
            }
        }
        if (resolvedRoles.isEmpty()) {
            resolvedRoles.add(createRoleIfMissing(createIfMissing));
        }
        return resolvedRoles;
    }

    private Set<Role> mergeRoles(Set<Role> first, Set<Role> second) {
        Set<Role> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return merged;
    }

    private Role createRoleIfMissing(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role saved = roleRepository.save(role);
        log.info("Role created by menu bootstrap: {}", saved.getName());
        return saved;
    }
}
