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

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseGet(this::createAdminRoleIfMissing);
        Role hrRole = roleRepository.findByNameIgnoreCase("ROLE_HR")
                .orElseGet(() -> createRoleIfMissing("ROLE_HR"));

        MstMenu adminMenu = upsertMenu(
                "Administration",
                null,
                "fa fa-user-shield",
                0,
                adminRole);

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
                adminRole, hrRole);

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
        upsertSubMenu(departmentMenu, "Manpower Applications", "/department/manpower/list", "fa fa-users-gear");
        upsertSubMenu(departmentMenu, "New Manpower Application", "/department/manpower/apply",
                "fa fa-file-circle-plus");
    }

    private void bootstrapRoles() {
        String[] roles = {
                "ROLE_USER", "ROLE_AGENCY", "ROLE_HR", "ROLE_STM", "ROLE_HOD2",
                "ROLE_HOD1", "ROLE_COO", "ROLE_PM", "ROLE_ADMIN", "ROLE_HOD3",
                "ROLE_STM1", "ROLE_DEPARTMENT", "ROLE_AUDITOR", "ROLE_EMPLOYEE",
                "ROLE_MAHAIT_ADMIN"
        };

        for (String roleName : roles) {
            roleRepository.findByNameIgnoreCase(roleName)
                    .orElseGet(() -> createRoleIfMissing(roleName));
        }
    }

    private Role createRoleIfMissing(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role saved = roleRepository.save(role);
        log.info("Role created: {}", saved.getName());
        return saved;
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

    private Role createAdminRoleIfMissing() {
        Role role = new Role();
        role.setName("ADMIN");
        Role saved = roleRepository.save(role);
        log.info("Role created by menu bootstrap: {}", saved.getName());
        return saved;
    }
}
