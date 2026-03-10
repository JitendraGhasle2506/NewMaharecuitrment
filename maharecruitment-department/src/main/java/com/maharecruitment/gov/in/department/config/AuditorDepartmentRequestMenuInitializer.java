package com.maharecruitment.gov.in.department.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;

@Component
@Order(41)
public class AuditorDepartmentRequestMenuInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditorDepartmentRequestMenuInitializer.class);

    private final MstMenuRepository mstMenuRepository;
    private final RoleRepository roleRepository;

    public AuditorDepartmentRequestMenuInitializer(
            MstMenuRepository mstMenuRepository,
            RoleRepository roleRepository) {
        this.mstMenuRepository = mstMenuRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role auditorRole = roleRepository.findByNameIgnoreCase("ROLE_AUDITOR")
                .or(() -> roleRepository.findByNameIgnoreCase("AUDITOR"))
                .orElseGet(() -> createRoleIfMissing("ROLE_AUDITOR"));

        upsertDirectMenu(
                "Department Audit Request",
                "/auditor/department-requests",
                "fa fa-magnifying-glass-chart",
                auditorRole);
    }

    private void upsertDirectMenu(
            String menuName,
            String url,
            String iconClass,
            Role roleToAssign) {
        MstMenu menu = mstMenuRepository.findByMenuNameEnglishIgnoreCase(menuName)
                .orElseGet(MstMenu::new);

        menu.setMenuNameEnglish(menuName);
        menu.setMenuNameMarathi(menuName);
        menu.setUrl(url);
        menu.setIcon(iconClass);
        menu.setIsSubMenu(1);
        menu.setIsActive("Y");

        Set<Role> roles = menu.getRoles();
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        if (roleToAssign != null) {
            roles.add(roleToAssign);
        }
        menu.setRoles(roles);

        MstMenu saved = mstMenuRepository.save(menu);
        log.info("Auditor menu bootstrapped. menuId={}, menuName={}, url={}",
                saved.getMenuId(),
                saved.getMenuNameEnglish(),
                saved.getUrl());
    }

    private Role createRoleIfMissing(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role saved = roleRepository.save(role);
        log.info("Role created for Auditor menu bootstrap: {}", saved.getName());
        return saved;
    }
}
